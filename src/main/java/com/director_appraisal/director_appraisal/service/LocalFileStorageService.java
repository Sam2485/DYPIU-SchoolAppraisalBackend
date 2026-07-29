package com.director_appraisal.director_appraisal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

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
        Path uploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        
        String cleanPath = objectName == null ? "" : objectName;
        if (cleanPath.contains("users/")) {
            cleanPath = cleanPath.substring(cleanPath.indexOf("users/"));
        } else if (cleanPath.contains("uploads/")) {
            cleanPath = cleanPath.substring(cleanPath.indexOf("uploads/") + "uploads/".length());
        }
        
        Path targetLocation = uploadDir.resolve(cleanPath).normalize();
        
        // Safety check to prevent Directory Traversal attacks
        if (targetLocation.startsWith(uploadDir) && Files.exists(targetLocation)) {
            return Files.newInputStream(targetLocation);
        }

        // Try direct objectName resolution if cleanPath didn't exist
        Path directLocation = uploadDir.resolve(objectName).normalize();
        if (directLocation.startsWith(uploadDir) && Files.exists(directLocation)) {
            return Files.newInputStream(directLocation);
        }

        // Fallback: Search for the exact filename anywhere under uploadDir
        try {
            String fileName = Paths.get(objectName).getFileName().toString();
            if (fileName != null && !fileName.isBlank()) {
                try (java.util.stream.Stream<Path> walk = Files.walk(uploadDir)) {
                    java.util.Optional<Path> found = walk
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().equalsIgnoreCase(fileName))
                            .findFirst();
                    if (found.isPresent()) {
                        return Files.newInputStream(found.get());
                    }
                }
            }
        } catch (Exception e) {
            // Fallback search error ignored
        }

        throw new IOException("File not found locally: " + targetLocation);
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
