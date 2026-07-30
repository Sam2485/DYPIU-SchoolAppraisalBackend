package com.director_appraisal.director_appraisal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Local filesystem implementation of StorageService.
 */
@Service
@Primary
public class LocalFileStorageService implements StorageService {

    private final String localUploadPath;

    public LocalFileStorageService(
            @Value("${app.upload.local-path}") String localUploadPath) {
        this.localUploadPath = localUploadPath;
    }

    @Override
    public String storeFile(String objectName, byte[] content) throws IOException {
        Path uploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path targetLocation = uploadDir.resolve(objectName).normalize();
        
        // Safety check to prevent Directory Traversal attacks
        if (!targetLocation.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid upload path: " + objectName);
        }

        Files.createDirectories(targetLocation.getParent());
        Files.write(targetLocation, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return "/uploads/" + objectName;
    }

    @Override
    public boolean deleteFile(String objectName) throws IOException {
        Path uploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        Path targetLocation = uploadDir.resolve(objectName).normalize();
        
        // Safety check to prevent Directory Traversal attacks
        if (!targetLocation.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid attachment path: " + objectName);
        }

        return Files.deleteIfExists(targetLocation);
    }

    @Override
    public InputStream downloadFile(String objectName) throws IOException {
        return downloadFile(objectName, null);
    }

    @Override
    public InputStream downloadFile(String objectName, String originalFileName) throws IOException {
        Path primaryUploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        
        List<Path> searchDirs = new java.util.ArrayList<>();
        searchDirs.add(primaryUploadDir);
        Path testDir = Paths.get("/app/uploads-test").toAbsolutePath().normalize();
        Path defaultDir = Paths.get("/app/uploads").toAbsolutePath().normalize();
        if (!searchDirs.contains(testDir)) searchDirs.add(testDir);
        if (!searchDirs.contains(defaultDir)) searchDirs.add(defaultDir);

        String cleanPath = objectName == null ? "" : objectName;
        if (cleanPath.contains("users/")) {
            cleanPath = cleanPath.substring(cleanPath.indexOf("users/"));
        } else if (cleanPath.contains("uploads-test/")) {
            cleanPath = cleanPath.substring(cleanPath.indexOf("uploads-test/") + "uploads-test/".length());
        } else if (cleanPath.contains("uploads/")) {
            cleanPath = cleanPath.substring(cleanPath.indexOf("uploads/") + "uploads/".length());
        }

        for (Path uploadDir : searchDirs) {
            if (!Files.exists(uploadDir)) continue;

            Path targetLocation = uploadDir.resolve(cleanPath).normalize();
            if (targetLocation.startsWith(uploadDir) && Files.exists(targetLocation)) {
                return Files.newInputStream(targetLocation);
            }

            Path directLocation = uploadDir.resolve(objectName == null ? "" : objectName).normalize();
            if (directLocation.startsWith(uploadDir) && Files.exists(directLocation)) {
                return Files.newInputStream(directLocation);
            }
        }

        // Fallback: Search for candidate filenames anywhere under all upload directories
        List<String> candidateNames = new java.util.ArrayList<>();
        if (objectName != null && !objectName.isBlank()) {
            candidateNames.add(Paths.get(objectName).getFileName().toString());
        }
        if (originalFileName != null && !originalFileName.isBlank()) {
            candidateNames.add(originalFileName.trim());
        }

        for (Path uploadDir : searchDirs) {
            if (!Files.exists(uploadDir)) continue;
            for (String candidate : candidateNames) {
                if (candidate.isBlank()) continue;
                String normCandidate = normalizeForSearch(candidate);

                try (java.util.stream.Stream<Path> walk = Files.walk(uploadDir)) {
                    java.util.Optional<Path> found = walk
                            .filter(Files::isRegularFile)
                            .filter(p -> {
                                String pName = p.getFileName().toString();
                                return pName.equalsIgnoreCase(candidate) ||
                                       (!normCandidate.isEmpty() && normalizeForSearch(pName).equalsIgnoreCase(normCandidate));
                            })
                            .findFirst();
                    if (found.isPresent()) {
                        return Files.newInputStream(found.get());
                    }
                } catch (Exception e) {
                    // Ignore search errors
                }
            }
        }

        throw new IOException("File not found locally: " + objectName);
    }

    private String normalizeForSearch(String str) {
        if (str == null) return "";
        // Strip UUID prefixes if present (e.g. 3613af12-95a3-4f32-aecc-53580b8d2502-)
        String cleaned = str.replaceAll("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}-", "");
        // Remove spaces, underscores, hyphens, and non-alphanumeric except extension dot
        return cleaned.toLowerCase().replaceAll("[^a-z0-9.]", "");
    }

    @Override
    public void deleteDirectory(String prefix) throws IOException {
        Path uploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        Path targetDir = uploadDir.resolve(prefix).normalize();
        
        // Safety check to prevent Directory Traversal attacks
        if (!targetDir.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid directory path: " + prefix);
        }

        if (Files.exists(targetDir)) {
            try (java.util.stream.Stream<Path> walk = Files.walk(targetDir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
            }
        }
    }
}
