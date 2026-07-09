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
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/pdf").build();
        storage.create(blobInfo, content);
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);
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
}
