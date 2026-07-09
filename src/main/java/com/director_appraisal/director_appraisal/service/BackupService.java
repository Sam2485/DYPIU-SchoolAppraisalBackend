package com.director_appraisal.director_appraisal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private final String localUploadPath;
    private final String backupPath;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    public BackupService(
            @Value("${app.upload.local-path}") String localUploadPath,
            @Value("${app.backup.path:${BACKUP_PATH:${app.upload.local-path}/backups}}") String backupPath) {
        this.localUploadPath = localUploadPath;
        this.backupPath = backupPath;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Uploads backup (ZIP)
    // ────────────────────────────────────────────────────────────────────────

    public void createUploadsBackup(OutputStream outputStream) throws IOException {
        Path sourceDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        if (!Files.exists(sourceDir)) {
            Files.createDirectories(sourceDir);
        }

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            Files.walk(sourceDir).forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        return;
                    }
                    // Exclude any backup archives from being recursively zipped
                    String filename = path.getFileName().toString().toLowerCase();
                    if (filename.endsWith(".zip") || filename.endsWith(".sql")) {
                        return;
                    }
                    String zipEntryName = sourceDir.relativize(path).toString().replace("\\", "/");
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException("Error writing zip entry: " + path, e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    public void restoreUploadsBackup(MultipartFile file) throws IOException {
        // Ensure the backup directory exists
        Path backupDir = Paths.get(backupPath).toAbsolutePath().normalize();
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        // Save a copy of the uploaded ZIP file to the VM backup folder as requested
        String originalFilename = file.getOriginalFilename();
        String savedBackupName = "uploads-backup-" + System.currentTimeMillis() + "-" + 
                (originalFilename != null ? originalFilename : "backup.zip");
        Path savedBackupPath = backupDir.resolve(savedBackupName).normalize();
        
        file.transferTo(savedBackupPath.toFile());

        // Extract the ZIP contents to the uploads folder
        Path targetDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(savedBackupPath))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = targetDir.resolve(entry.getName()).normalize();
                
                // Security check against Zip Slip (directory traversal vulnerability)
                if (!newPath.startsWith(targetDir)) {
                    throw new IOException("Entry is outside of the target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    try (OutputStream os = Files.newOutputStream(newPath)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Database backup (SQL)
    // ────────────────────────────────────────────────────────────────────────

    public byte[] createDatabaseBackup() throws IOException {
        String dbName = extractDbNameFromUrl(dbUrl);
        String host = extractHostFromUrl(dbUrl);
        String port = extractPortFromUrl(dbUrl);

        ProcessBuilder pb;
        if (dbUrl.contains("socketFactory")) {
            // GCP Cloud SQL connection, use standard pg_dump via default socket settings
            pb = new ProcessBuilder(
                    "pg_dump",
                    "-U", dbUsername,
                    "-F", "p",
                    "-b",
                    "-v",
                    dbName
            );
        } else {
            // Standard TCP connection
            pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", host,
                    "-p", port,
                    "-U", dbUsername,
                    "-F", "p",
                    "-b",
                    "-v",
                    dbName
            );
        }

        pb.environment().put("PGPASSWORD", dbPassword);

        Process process = pb.start();
        try (InputStream is = process.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // Read error output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder err = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        err.append(line).append("\n");
                    }
                    throw new IOException("pg_dump failed with exit code " + exitCode + ". Error: " + err.toString());
                }
            }
            return baos.toByteArray();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("pg_dump process was interrupted", e);
        }
    }

    public void restoreDatabaseBackup(MultipartFile file) throws IOException {
        // Ensure the backup directory exists and save a copy of the SQL file there
        Path backupDir = Paths.get(backupPath).toAbsolutePath().normalize();
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        String originalFilename = file.getOriginalFilename();
        String savedBackupName = "db-backup-" + System.currentTimeMillis() + "-" + 
                (originalFilename != null ? originalFilename : "backup.sql");
        Path savedSqlPath = backupDir.resolve(savedBackupName).normalize();
        file.transferTo(savedSqlPath.toFile());

        String dbName = extractDbNameFromUrl(dbUrl);
        String host = extractHostFromUrl(dbUrl);
        String port = extractPortFromUrl(dbUrl);

        ProcessBuilder pb;
        if (dbUrl.contains("socketFactory")) {
            pb = new ProcessBuilder(
                    "psql",
                    "-U", dbUsername,
                    "-d", dbName,
                    "-f", savedSqlPath.toString()
            );
        } else {
            pb = new ProcessBuilder(
                    "psql",
                    "-h", host,
                    "-p", port,
                    "-U", dbUsername,
                    "-d", dbName,
                    "-f", savedSqlPath.toString()
            );
        }

        pb.environment().put("PGPASSWORD", dbPassword);

        Process process = pb.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder err = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        err.append(line).append("\n");
                    }
                    throw new IOException("psql failed with exit status " + exitCode + ". Error: " + err.toString());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("psql restore process was interrupted", e);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Parsing helpers for JDBC URL
    // ────────────────────────────────────────────────────────────────────────

    private String extractDbNameFromUrl(String url) {
        // e.g. jdbc:postgresql://localhost:5432/school_appraisal or jdbc:postgresql:///school_appraisal?...
        String cleanUrl = url.split("\\?")[0];
        int lastSlash = cleanUrl.lastIndexOf('/');
        return cleanUrl.substring(lastSlash + 1);
    }

    private String extractHostFromUrl(String url) {
        if (url.startsWith("jdbc:postgresql:///")) {
            return "localhost";
        }
        try {
            String hostPortPart = url.substring("jdbc:postgresql://".length()).split("/")[0];
            return hostPortPart.split(":")[0];
        } catch (Exception e) {
            return "localhost";
        }
    }

    private String extractPortFromUrl(String url) {
        if (url.startsWith("jdbc:postgresql:///")) {
            return "5432";
        }
        try {
            String hostPortPart = url.substring("jdbc:postgresql://".length()).split("/")[0];
            String[] parts = hostPortPart.split(":");
            return parts.length > 1 ? parts[1] : "5432";
        } catch (Exception e) {
            return "5432";
        }
    }
}
