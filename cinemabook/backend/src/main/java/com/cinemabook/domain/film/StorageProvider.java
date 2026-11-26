package com.cinemabook.domain.film;

import java.io.InputStream;
import java.time.Duration;

/**
 * Open/Closed Principle — core storage abstraction.
 *
 * Implementations: LocalStorageProvider (dev), S3StorageProvider (prod), R2StorageProvider.
 * Adding a new provider = new class only. This interface never changes.
 *
 * Dependency Inversion: FilmService depends on THIS, never on AWS SDK directly.
 */
public interface StorageProvider {

    /**
     * Stores a file and returns the storage key (object path).
     *
     * @param inputStream  file content
     * @param key          desired object key (e.g. "films/uuid/movie.mp4")
     * @param contentType  MIME type (e.g. "video/mp4")
     * @param sizeBytes    total byte length of the stream
     * @return the storage key as stored
     */
    String store(InputStream inputStream, String key,
                 String contentType, long sizeBytes);

    /**
     * Generates a short-lived pre-signed URL for reading a private object.
     *
     * @param key       object key
     * @param expiresIn how long the URL should remain valid
     * @return a URL the client can use directly
     */
    String generatePresignedUrl(String key, Duration expiresIn);

    /**
     * Generates a pre-signed URL for direct client-to-storage upload.
     * Used for large film files — avoids routing through the API server.
     *
     * @param key       target object key
     * @param expiresIn URL validity duration
     * @return a PUT URL the client uploads directly to
     */
    String generateUploadPresignedUrl(String key, Duration expiresIn);

    /**
     * Deletes an object from storage.
     *
     * @param key object key to delete
     */
    void delete(String key);

    /**
     * Returns true if an object with this key exists.
     */
    boolean exists(String key);
}