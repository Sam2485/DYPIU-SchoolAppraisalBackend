package com.director_appraisal.director_appraisal.service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstraction layer for file storage operations, allowing transparent switching
 * between cloud storage (Google Cloud Storage) and local disk storage.
 */
public interface StorageService {

    /**
     * Stores a file under the specified object name.
     *
     * @param objectName the path/key under which the file should be stored
     * @param content    the binary content of the file
     * @return the URL or path used to reference/retrieve the stored file
     * @throws IOException if an error occurs during file storage
     */
    String storeFile(String objectName, byte[] content) throws IOException;

    /**
     * Deletes a file matching the specified object name.
     *
     * @param objectName the path/key of the file to delete
     * @return true if the file was deleted, false otherwise
     * @throws IOException if an error occurs during file deletion
     */
    boolean deleteFile(String objectName) throws IOException;

    /**
     * Downloads/reads a file's content as an input stream.
     *
     * @param objectName the path/key of the file to download
     * @return an InputStream for reading the file content
     * @throws IOException if the file is not found or cannot be read
     */
    InputStream downloadFile(String objectName) throws IOException;
}
