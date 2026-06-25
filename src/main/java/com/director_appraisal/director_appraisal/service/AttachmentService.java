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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        if (fileExists(candidate.objectName)) {
            throw new IllegalArgumentException("This file has already been uploaded.");
        }
        return storeFile(candidate);
    }

    public List<AttachmentResponse> uploadFiles(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one file is required.");
        }

        for (MultipartFile file : files) {
            validateFile(file);
        }

        Map<String, UploadCandidate> candidates = new LinkedHashMap<>();
        for (MultipartFile file : files) {
            UploadCandidate candidate = buildUploadCandidate(file);
            if (candidates.putIfAbsent(candidate.objectName, candidate) != null) {
                throw new IllegalArgumentException("Duplicate file selected: " + candidate.originalFilename);
            }
        }

        for (UploadCandidate candidate : candidates.values()) {
            if (fileExists(candidate.objectName)) {
                throw new IllegalArgumentException("This file has already been uploaded: " + candidate.originalFilename);
            }
        }

        List<AttachmentResponse> responses = new ArrayList<>();
        for (UploadCandidate candidate : candidates.values()) {
            responses.add(storeFile(candidate));
        }
        return responses;
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
        String contentHash = hashSha256(content);
        String objectName = "users/" + getCurrentUserKey() + "/attachments/" + contentHash + ".pdf";
        return new UploadCandidate(originalFilename, objectName, content);
    }

    private boolean fileExists(String objectName) {
        if (useGcp) {
            return storage.get(BlobId.of(bucketName, objectName)) != null;
        }

        Path uploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        Path targetLocation = uploadDir.resolve(objectName).normalize();
        return Files.exists(targetLocation);
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
