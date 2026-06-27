package com.director_appraisal.director_appraisal.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AttachmentService {

    private final String bucketName;
    private final String localUploadPath;
    private Storage storage;
    private boolean useGcp = false;

    public AttachmentService(
            @Value("${app.gcp.bucket-name}") String bucketName,
            @Value("${app.upload.local-path}") String localUploadPath,
            @Value("${app.gcp.enabled:false}") boolean gcpEnabled,
            @Value("${USE_LOCAL_STORAGE:true}") boolean useLocalStorage) {
        this.bucketName = bucketName;
        this.localUploadPath = localUploadPath;
        this.useGcp = gcpEnabled || !useLocalStorage;

        if (this.useGcp) {
            try {
                // Initialize GCP Cloud Storage client
                this.storage = StorageOptions.getDefaultInstance().getService();
                System.out.println("GCP Cloud Storage successfully initialized with bucket: " + bucketName);
            } catch (Exception e) {
                System.err.println("Failed to initialize GCP Storage client: " + e.getMessage() + ". Falling back to local storage.");
                this.useGcp = false;
            }
        } else {
            System.out.println("GCP Storage disabled. Using local storage fallback.");
        }
    }

    public AttachmentResponse uploadFile(MultipartFile file) throws IOException {
        validateFile(file);
        UploadCandidate candidate = buildUploadCandidate(file);
        return storeFile(candidate);
    }

    public List<AttachmentResponse> uploadFiles(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one file is required.");
        }

        for (MultipartFile file : files) {
            validateFile(file);
        }

        List<AttachmentResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(storeFile(buildUploadCandidate(file)));
        }
        return responses;
    }

    public boolean deleteFile(String fileUrl) throws IOException {
        String objectName = extractObjectName(fileUrl);

        if (useGcp) {
            return storage.delete(BlobId.of(bucketName, objectName));
        }

        Path uploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        Path targetLocation = uploadDir.resolve(objectName).normalize();
        if (!targetLocation.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid attachment URL.");
        }
        return Files.deleteIfExists(targetLocation);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }

        // Validate size
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB.");
        }

        // Validate type (PDF only)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Invalid file type. Only PDF files are allowed.");
        }
    }

    private UploadCandidate buildUploadCandidate(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        byte[] content = file.getBytes();
        String objectName = "users/" + getCurrentUserKey() + "/attachments/" + UUID.randomUUID() + "-" + sanitizeFilename(originalFilename);
        return new UploadCandidate(originalFilename, objectName, content);
    }

    private String sanitizeFilename(String filename) {
        String normalizedFilename = filename.replace("\\", "/");
        int lastSlashIndex = normalizedFilename.lastIndexOf('/');
        String baseFilename = lastSlashIndex >= 0 ? normalizedFilename.substring(lastSlashIndex + 1) : normalizedFilename;
        String safeFilename = baseFilename.replaceAll("[^A-Za-z0-9._-]", "_");
        return safeFilename.isBlank() ? "attachment.pdf" : safeFilename;
    }

    private String extractObjectName(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("Attachment URL is required.");
        }

        String objectName;
        if (fileUrl.startsWith("/uploads/")) {
            objectName = fileUrl.substring("/uploads/".length());
        } else {
            URI uri;
            try {
                uri = URI.create(fileUrl);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid attachment URL.");
            }

            String host = uri.getHost();
            String path = uri.getPath();
            String storageHostPrefix = bucketName + ".storage.googleapis.com";
            String storagePathPrefix = "/" + bucketName + "/";

            if (path != null && path.startsWith("/uploads/")) {
                objectName = path.substring("/uploads/".length());
            } else if ("storage.googleapis.com".equalsIgnoreCase(host) && path != null && path.startsWith(storagePathPrefix)) {
                objectName = path.substring(storagePathPrefix.length());
            } else if (storageHostPrefix.equalsIgnoreCase(host) && path != null && path.length() > 1) {
                objectName = path.substring(1);
            } else {
                throw new IllegalArgumentException("Invalid attachment URL.");
            }
        }

        String userPrefix = "users/" + getCurrentUserKey() + "/attachments/";
        if (!objectName.startsWith(userPrefix)) {
            throw new IllegalArgumentException("You can only delete your own uploaded files.");
        }
        return objectName;
    }

    private AttachmentResponse storeFile(UploadCandidate candidate) throws IOException {
        if (useGcp) {
            // Upload to Google Cloud Storage bucket
            BlobId blobId = BlobId.of(bucketName, candidate.objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/pdf").build();
            storage.create(blobInfo, candidate.content);

            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, candidate.objectName);
            return new AttachmentResponse(candidate.originalFilename, publicUrl);
        } else {
            // Local storage fallback (creates folder if not exists)
            Path uploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path targetLocation = uploadDir.resolve(candidate.objectName).normalize();
            Files.createDirectories(targetLocation.getParent());
            Files.write(targetLocation, candidate.content, StandardOpenOption.CREATE_NEW);

            // Serve local file using local controller mapping /uploads/**
            String fileUrl = "/uploads/" + candidate.objectName;
            return new AttachmentResponse(candidate.originalFilename, fileUrl);
        }
    }

    private String getCurrentUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null && authentication.getName() != null ? authentication.getName() : "anonymous";
        return hashSha256(username.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)).substring(0, 16);
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
            throw new RuntimeException("Error hashing file.", e);
        }
    }

    private static class UploadCandidate {
        private final String originalFilename;
        private final String objectName;
        private final byte[] content;

        private UploadCandidate(String originalFilename, String objectName, byte[] content) {
            this.originalFilename = originalFilename;
            this.objectName = objectName;
            this.content = content;
        }
    }

    public java.io.InputStream downloadAttachmentStream(String fileUrl) throws IOException {
        String objectName;
        if (fileUrl.startsWith("/uploads/")) {
            objectName = fileUrl.substring("/uploads/".length());
        } else {
            URI uri;
            try {
                uri = URI.create(fileUrl);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid attachment URL.");
            }

            String host = uri.getHost();
            String path = uri.getPath();
            String storageHostPrefix = bucketName + ".storage.googleapis.com";
            String storagePathPrefix = "/" + bucketName + "/";

            if (path != null && path.startsWith("/uploads/")) {
                objectName = path.substring("/uploads/".length());
            } else if ("storage.googleapis.com".equalsIgnoreCase(host) && path != null && path.startsWith(storagePathPrefix)) {
                objectName = path.substring(storagePathPrefix.length());
            } else if (storageHostPrefix.equalsIgnoreCase(host) && path != null && path.length() > 1) {
                objectName = path.substring(1);
            } else {
                objectName = path != null ? path : fileUrl;
            }
        }

        if (objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }

        if (useGcp) {
            com.google.cloud.storage.Blob blob = storage.get(BlobId.of(bucketName, objectName));
            if (blob == null) {
                throw new IOException("File not found in GCP bucket: " + objectName);
            }
            return java.nio.channels.Channels.newInputStream(blob.reader());
        }

        Path uploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        Path targetLocation = uploadDir.resolve(objectName).normalize();
        if (!targetLocation.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid attachment URL.");
        }
        if (!Files.exists(targetLocation)) {
            throw new IOException("File not found locally: " + targetLocation);
        }
        return Files.newInputStream(targetLocation);
    }

    public static class AttachmentResponse {
        private final String name;
        private final String url;

        public AttachmentResponse(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }
}
