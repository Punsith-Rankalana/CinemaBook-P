package com.cinemabook.infrastructure.storage;

import com.cinemabook.domain.film.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;

/**
 * AWS S3 / Cloudflare R2 storage provider for production.
 *
 * Open/Closed Principle: FilmService never changes — only this class
 * is activated instead of LocalStorageProvider when profile = "prod".
 *
 * To activate: set spring.profiles.active=prod in application.properties
 * and add the AWS SDK dependency to pom.xml (see comment below).
 *
 * pom.xml addition needed for prod:
 * <dependency>
 *   <groupId>software.amazon.awssdk</groupId>
 *   <artifactId>s3</artifactId>
 *   <version>2.25.60</version>
 * </dependency>
 */
@Service
@Profile("prod")
@Slf4j
public class S3StorageProvider implements StorageProvider {

    // TODO Phase 2 prod wiring:
    // @Value("${cinemabook.storage.s3.bucket}") private String bucket;
    // @Value("${cinemabook.storage.s3.region}") private String region;
    // private S3Client s3Client;
    // private S3Presigner presigner;

    @Override
    public String store(InputStream inputStream, String key,
                        String contentType, long sizeBytes) {
        // TODO: s3Client.putObject(PutObjectRequest, RequestBody)
        throw new UnsupportedOperationException("S3StorageProvider: wire AWS SDK for prod");
    }

    @Override
    public String generatePresignedUrl(String key, Duration expiresIn) {
        // TODO: presigner.presignGetObject(...)
        throw new UnsupportedOperationException("S3StorageProvider: wire AWS SDK for prod");
    }

    @Override
    public String generateUploadPresignedUrl(String key, Duration expiresIn) {
        // TODO: presigner.presignPutObject(...)
        throw new UnsupportedOperationException("S3StorageProvider: wire AWS SDK for prod");
    }

    @Override
    public void delete(String key) {
        // TODO: s3Client.deleteObject(...)
        throw new UnsupportedOperationException("S3StorageProvider: wire AWS SDK for prod");
    }

    @Override
    public boolean exists(String key) {
        // TODO: s3Client.headObject(...)
        throw new UnsupportedOperationException("S3StorageProvider: wire AWS SDK for prod");
    }
}