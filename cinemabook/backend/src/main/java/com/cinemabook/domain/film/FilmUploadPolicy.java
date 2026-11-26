package com.cinemabook.domain.film;

/**
 * Encapsulates upload validation rules.
 *
 * Open/Closed: add new rules (e.g. DRM check, virus scan) as new
 * implementations or decorators — never edit existing policy classes.
 */
public interface FilmUploadPolicy {

    long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024 * 1024; // 10 GB

    /** Allowed video MIME types. */
    java.util.Set<String> ALLOWED_CONTENT_TYPES = java.util.Set.of(
            "video/mp4",
            "video/x-matroska",   // .mkv
            "video/quicktime",    // .mov
            "video/x-msvideo"     // .avi
    );

    /**
     * Validates a proposed upload.
     * Throws {@link com.cinemabook.shared.exception.BusinessRuleException} if invalid.
     *
     * @param contentType MIME type of the file
     * @param sizeBytes   file size in bytes
     * @param ownerId     the film owner attempting the upload
     */
    void validate(String contentType, long sizeBytes, java.util.UUID ownerId);
}