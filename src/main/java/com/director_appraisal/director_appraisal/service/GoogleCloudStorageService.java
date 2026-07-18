package com.director_appraisal.director_appraisal.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;

/**
 * Google Cloud Storage implementation of StorageService.
 * Active under the "gcp" profile.
 */
@Service
@Profile("gcp")
public class GoogleCloudStorageService implements StorageService {

    private final String bucketName;
    private final Storage storage;

    public GoogleCloudStorageService(
            @Value("${app.gcp.bucket-name}") String bucketName) {
        this.bucketName = bucketName;
        // Lazily retrieves the default Google Cloud Storage service instance
        // This will authenticate using Application Default Credentials (ADC) in GCP environment
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    @Override
    public String storeFile(String objectName, byte[] content) throws IOException {
        BlobId blobId = BlobId.of(bucketName, objectName);
        String contentType = determineContentType(objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
        storage.create(blobInfo, content);
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);
    }

    private String determineContentType(String objectName) {
        if (objectName == null) {
            return "application/octet-stream";
        }
        String lower = objectName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lower.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lower.endsWith(".doc")) {
            return "application/msword";
        } else if (lower.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }

    @Override
    public boolean deleteFile(String objectName) throws IOException {
        return storage.delete(BlobId.of(bucketName, objectName));
    }

    @Override
    public InputStream downloadFile(String objectName) throws IOException {
        com.google.cloud.storage.Blob blob = storage.get(BlobId.of(bucketName, objectName));
        if (blob == null) {
            throw new IOException("File not found in GCP bucket: " + objectName);
        }
        return Channels.newInputStream(blob.reader());
    }

    @Override
    public void deleteDirectory(String prefix) throws IOException {
        com.google.api.gax.paging.Page<com.google.cloud.storage.Blob> blobs = storage.list(
                bucketName,
                Storage.BlobListOption.prefix(prefix)
        );
        for (com.google.cloud.storage.Blob blob : blobs.iterateAll()) {
            storage.delete(blob.getBlobId());
        }
    }
}
