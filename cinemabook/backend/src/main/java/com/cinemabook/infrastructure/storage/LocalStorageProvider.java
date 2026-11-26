package com.cinemabook.infrastructure.storage;

import com.cinemabook.domain.film.StorageProvider;
import com.cinemabook.shared.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * LOCAL storage provider for development.
 * Stores files on the local filesystem under ${cinemabook.storage.local-path}.
 *
 * Active only when Spring profile is "local" or "default" (not "prod").
 *
 * Open/Closed: swap for S3StorageProvider in prod by changing the active profile.
 * Zero code changes to FilmService.
 */
@Service
@Profile({"default", "local", "test"})
@Slf4j
public class LocalStorageProvider implements StorageProvider {

    private final Path basePath;
    private final String baseUrl;

    public LocalStorageProvider(
            @Value("${cinemabook.storage.local-path:${user.home}/cinemabook-storage}") String localPath,
            @Value("${cinemabook.storage.local-base-url:http://localhost:8080/api/v1/files}") String baseUrl) {

        this.basePath = Paths.get(localPath);
        this.baseUrl  = baseUrl;

        try {
            Files.createDirectories(this.basePath);
            log.info("LocalStorageProvider initialised at: {}", this.basePath);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create storage directory: " + localPath, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String key,
                        String contentType, long sizeBytes) {
        try {
            Path target = basePath.resolve(key);
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file: key={} size={}", key, sizeBytes);
            return key;
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public String generatePresignedUrl(String key, Duration expiresIn) {
        // In dev, return a direct API URL — no expiry needed locally
        return baseUrl + "/" + key;
    }

    @Override
    public String generateUploadPresignedUrl(String key, Duration expiresIn) {
        // In dev, return the API upload endpoint — client posts to backend
        return baseUrl + "/upload/" + key;
    }

    @Override
    public void delete(String key) {
        try {
            Path target = basePath.resolve(key);
            Files.deleteIfExists(target);
            log.debug("Deleted file: key={}", key);
        } catch (IOException e) {
            log.warn("Failed to delete file key={}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(basePath.resolve(key));
    }
}