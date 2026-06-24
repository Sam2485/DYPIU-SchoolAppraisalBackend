package com.director_appraisal.director_appraisal.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        // Validate size
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB.");
        }

        // Validate type (PDF only)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Invalid file type. Only PDF files are allowed.");
        }

        String uniqueFilename = UUID.randomUUID().toString() + "-" + originalFilename;

        if (useGcp) {
            // Upload to Google Cloud Storage bucket
            BlobId blobId = BlobId.of(bucketName, uniqueFilename);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/pdf").build();
            storage.create(blobInfo, file.getBytes());

            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, uniqueFilename);
            return new AttachmentResponse(originalFilename, publicUrl);
        } else {
            // Local storage fallback (creates folder if not exists)
            Path uploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path targetLocation = uploadDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Serve local file using local controller mapping /uploads/**
            String fileUrl = "/uploads/" + uniqueFilename;
            return new AttachmentResponse(originalFilename, fileUrl);
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
