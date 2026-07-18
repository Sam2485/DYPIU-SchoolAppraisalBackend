package com.director_appraisal.director_appraisal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Local filesystem implementation of StorageService.
 * Active under "vm" or fallback "default" profile.
 */
@Service
@Profile({"vm", "default"})
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
        Path targetLocation = uploadDir.resolve(objectName).normalize();
        
        // Safety check to prevent Directory Traversal attacks
        if (!targetLocation.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid attachment path: " + objectName);
        }

        if (!Files.exists(targetLocation)) {
            throw new IOException("File not found locally: " + targetLocation);
        }

        return Files.newInputStream(targetLocation);
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
