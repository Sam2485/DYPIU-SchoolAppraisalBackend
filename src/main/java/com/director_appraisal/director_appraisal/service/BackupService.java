package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final String localUploadPath;
    private final String backupPath;
    private final UserRepository userRepository;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    public BackupService(
            @Value("${app.upload.local-path}") String localUploadPath,
            @Value("${app.backup.path:${BACKUP_PATH:${app.upload.local-path}/backups}}") String backupPath,
            UserRepository userRepository) {
        // Clean environment variable values that might contain wrapping double quotes from Docker env-file parser
        this.localUploadPath = cleanPathQuotes(localUploadPath);
        this.backupPath = cleanPathQuotes(backupPath);
        this.userRepository = userRepository;
        log.info("Initialized BackupService. Local upload path: '{}', Backup path: '{}'", this.localUploadPath, this.backupPath);
    }

    private String cleanPathQuotes(String path) {
        if (path == null) return null;
        String cleaned = path.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.startsWith("'") && cleaned.endsWith("'")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned.trim();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Uploads backup (ZIP)
    // ────────────────────────────────────────────────────────────────────────

    public void createUploadsBackup(OutputStream outputStream) throws IOException {
        Path sourceDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        log.info("Starting zipping of uploads directory: '{}'", sourceDir);
        if (!Files.exists(sourceDir)) {
            log.info("Uploads directory does not exist, creating it: '{}'", sourceDir);
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
                        log.debug("Skipping backup/restore system file during walk: {}", filename);
                        return;
                    }
                    String zipEntryName = sourceDir.relativize(path).toString().replace("\\", "/");
                    log.info("Adding zip entry: {}", zipEntryName);
                    
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    log.error("Failed to write zip entry for path: '{}'", path, e);
                    throw new RuntimeException("Error writing zip entry: " + path, e);
                }
            });
            log.info("Successfully finished zipping uploads directory.");
        } catch (RuntimeException e) {
            log.error("Error during walking/zipping uploads directory", e);
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    public void restoreUploadsBackup(MultipartFile file) throws IOException {
        Path backupDir = Paths.get(backupPath).toAbsolutePath().normalize();
        log.info("Restoring uploads backup. Active backup directory: '{}'", backupDir);
        if (!Files.exists(backupDir)) {
            log.info("Backup directory does not exist, creating it: '{}'", backupDir);
            Files.createDirectories(backupDir);
        }

        // Save a copy of the uploaded ZIP file to the VM backup folder as requested
        String originalFilename = file.getOriginalFilename();
        String savedBackupName = "uploads-backup-" + System.currentTimeMillis() + "-" + 
                (originalFilename != null ? originalFilename : "backup.zip");
        Path savedBackupPath = backupDir.resolve(savedBackupName).normalize();
        try {
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, savedBackupPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to copy uploaded ZIP file to target path '{}'. Error: {}", savedBackupPath, e.getMessage(), e);
            throw e;
        }
        log.info("ZIP copy completed successfully. File size: {} bytes", Files.size(savedBackupPath));

        // Extract the ZIP contents to the uploads folder
        Path targetDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        log.info("Extracting ZIP contents to target uploads folder: '{}'", targetDir);
        if (!Files.exists(targetDir)) {
            log.info("Target uploads directory does not exist, creating it: '{}'", targetDir);
            Files.createDirectories(targetDir);
        }

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(savedBackupPath))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            int count = 0;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                log.info("Processing zip entry: '{}'", entryName);
                
                // If ZIP was created with the top-level 'uploads' folder, strip it
                if (entryName.startsWith("uploads/")) {
                    entryName = entryName.substring("uploads/".length());
                } else if (entryName.startsWith("uploads\\")) {
                    entryName = entryName.substring("uploads\\".length());
                }
                
                if (entryName.isEmpty()) {
                    log.info("Skipping empty top-level directory entry");
                    continue;
                }

                Path newPath = targetDir.resolve(entryName).normalize();
                log.info("Resolved entry target path: '{}'", newPath);
                
                // Security check against Zip Slip (directory traversal vulnerability)
                if (!newPath.startsWith(targetDir)) {
                    log.error("Zip Slip security check failed for entry: '{}'! Resolved path: '{}' is outside of target directory: '{}'", 
                            entry.getName(), newPath, targetDir);
                    throw new IOException("Entry is outside of the target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    log.info("Creating nested directory: '{}'", newPath);
                    Files.createDirectories(newPath);
                } else {
                    log.info("Writing file (overwriting if exists): '{}'", newPath);
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                    count++;
                }
                zis.closeEntry();
            }
            log.info("Successfully extracted {} files from uploads ZIP archive.", count);
            organizeUploadsDirectories();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Database backup (SQL)
    // ────────────────────────────────────────────────────────────────────────

    public byte[] createDatabaseBackup() throws IOException {
        String dbName = extractDbNameFromUrl(dbUrl);
        String host = extractHostFromUrl(dbUrl);
        String port = extractPortFromUrl(dbUrl);

        log.info("Starting database backup dump. DB Name: '{}', Host: '{}', Port: '{}'", dbName, host, port);

        ProcessBuilder pb;
        if (dbUrl.contains("socketFactory")) {
            log.info("GCP Cloud SQL connection detected. Using socketpg_dump command.");
            pb = new ProcessBuilder(
                    "pg_dump",
                    "-U", dbUsername,
                    "-F", "p",
                    "-b",
                    "-v",
                    dbName
            );
        } else {
            log.info("Standard TCP database connection detected. Using standard TCP command.");
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
            log.info("pg_dump process completed with exit code: {}", exitCode);
            
            if (exitCode != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder err = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        err.append(line).append("\n");
                    }
                    log.error("pg_dump process failed: {}", err);
                    throw new IOException("pg_dump failed with exit code " + exitCode + ". Error: " + err.toString());
                }
            }
            return baos.toByteArray();
        } catch (InterruptedException e) {
            log.error("pg_dump process interrupted", e);
            Thread.currentThread().interrupt();
            throw new IOException("pg_dump process was interrupted", e);
        }
    }

    public void restoreDatabaseBackup(MultipartFile file) throws IOException {
        Path backupDir = Paths.get(backupPath).toAbsolutePath().normalize();
        log.info("Restoring database backup. Active backup directory: '{}'", backupDir);
        if (!Files.exists(backupDir)) {
            log.info("Backup directory does not exist, creating it: '{}'", backupDir);
            Files.createDirectories(backupDir);
        }

        String originalFilename = file.getOriginalFilename();
        String savedBackupName = "db-backup-" + System.currentTimeMillis() + "-" + 
                (originalFilename != null ? originalFilename : "backup.sql");
        Path savedSqlPath = backupDir.resolve(savedBackupName).normalize();
        try {
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, savedSqlPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to copy uploaded SQL file to target path '{}'. Error: {}", savedSqlPath, e.getMessage(), e);
            throw e;
        }
        log.info("SQL copy completed successfully. File size: {} bytes", Files.size(savedSqlPath));

        String dbName = extractDbNameFromUrl(dbUrl);
        String host = extractHostFromUrl(dbUrl);
        String port = extractPortFromUrl(dbUrl);
        log.info("Executing psql restore to Database: '{}', Host: '{}', Port: '{}'", dbName, host, port);

        ProcessBuilder pb;
        if (dbUrl.contains("socketFactory")) {
            log.info("Using GCP Cloud SQL socket restore psql config.");
            pb = new ProcessBuilder(
                    "psql",
                    "-U", dbUsername,
                    "-d", dbName,
                    "-f", savedSqlPath.toString()
            );
        } else {
            log.info("Using standard TCP restore psql config.");
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
            log.info("psql restore process completed with exit code: {}", exitCode);
            if (exitCode != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder err = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        err.append(line).append("\n");
                    }
                    log.error("psql restore process failed: {}", err);
                    throw new IOException("psql failed with exit status " + exitCode + ". Error: " + err.toString());
                }
            }
        } catch (InterruptedException e) {
            log.error("psql restore process interrupted", e);
            Thread.currentThread().interrupt();
            throw new IOException("psql restore process was interrupted", e);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Parsing helpers for JDBC URL
    // ────────────────────────────────────────────────────────────────────────

    private String extractDbNameFromUrl(String url) {
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

    private String cleanAdministrativePostName(String post) {
        if (post == null || post.isBlank()) {
            return "";
        }
        return post.trim().toLowerCase().replace(" ", "_").replace("-", "_");
    }

    private String getReadableName(com.director_appraisal.director_appraisal.model.User user) {
        String post = user.getPost();
        String school = user.getSchool();
        String role = user.getRole();
        String email = user.getEmail();

        if (post != null && !post.trim().isEmpty()) {
            return cleanAdministrativePostName(post);
        } else if (school != null && !school.trim().isEmpty()) {
            return school.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        } else if (role != null && !role.trim().isEmpty()) {
            return role.trim().toLowerCase().replace(" ", "_").replace("-", "_");
        } else if (email != null && !email.trim().isEmpty()) {
            return email.split("@")[0].toLowerCase();
        }
        return "unknown";
    }

    private void mergeDirectories(Path src, Path dst) throws IOException {
        if (!Files.exists(dst)) {
            Files.createDirectories(dst);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(src)) {
            for (Path entry : stream) {
                Path target = dst.resolve(entry.getFileName());
                if (Files.isDirectory(entry)) {
                    mergeDirectories(entry, target);
                } else {
                    Files.move(entry, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Files.delete(src);
    }

    private String hashSha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing email.", e);
        }
    }

    @PostConstruct
    public void organizeUploadsDirectories() {
        log.info("Running automatic uploads directory organization...");
        Path baseDir = Paths.get(localUploadPath, "users").toAbsolutePath().normalize();
        if (!Files.exists(baseDir)) {
            log.warn("Uploads base directory does not exist yet: '{}'", baseDir);
            return;
        }

        try {
            for (com.director_appraisal.director_appraisal.model.User user : userRepository.findAll()) {
                String email = user.getEmail();
                if (email == null || email.isBlank()) {
                    continue;
                }

                String hashKey = hashSha256(email.trim().toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0, 16);
                String readableName = getReadableName(user);
                if ("unknown".equals(readableName)) {
                    continue;
                }

                Path hashPath = baseDir.resolve(hashKey).normalize();
                Path readablePath = baseDir.resolve(readableName).normalize();

                log.info("Checking mapping: {} -> {}", hashKey, readableName);

                // Case 1: Hash path is already a symlink
                if (Files.isSymbolicLink(hashPath)) {
                    Path target = Files.readSymbolicLink(hashPath);
                    if (!target.getFileName().toString().equals(readableName)) {
                        log.info("Updating symlink: {} -> {}", hashKey, readableName);
                        Files.delete(hashPath);
                        Files.createSymbolicLink(hashPath, Paths.get(readableName));
                    }
                }
                // Case 2: Hash path exists as a real directory
                else if (Files.exists(hashPath)) {
                    if (Files.exists(readablePath)) {
                        log.info("Target folder {} already exists, merging...", readableName);
                        mergeDirectories(hashPath, readablePath);
                    } else {
                        log.info("Renaming {} -> {}", hashKey, readableName);
                        Files.move(hashPath, readablePath);
                    }
                    Files.createSymbolicLink(hashPath, Paths.get(readableName));
                }
                // Case 3: Target path exists but symlink is missing
                else if (Files.exists(readablePath)) {
                    log.info("Creating missing symlink: {} -> {}", hashKey, readableName);
                    Files.createSymbolicLink(hashPath, Paths.get(readableName));
                }
            }
        } catch (Exception e) {
            log.error("Failed to organize uploads directories: {}", e.getMessage(), e);
        }
    }
}
