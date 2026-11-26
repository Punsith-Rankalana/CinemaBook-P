package com.cinemabook.domain.film;

import com.cinemabook.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default upload validation.
 * Open/Closed: override or replace this bean in tests or for new rules.
 */
@Component
public class DefaultFilmUploadPolicy implements FilmUploadPolicy {

    @Override
    public void validate(String contentType, long sizeBytes, UUID ownerId) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleException(
                    "Unsupported file type: " + contentType +
                            ". Allowed: MP4, MKV, MOV, AVI");
        }

        if (sizeBytes <= 0) {
            throw new BusinessRuleException("File size must be greater than zero");
        }

        if (sizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException(
                    "File size " + (sizeBytes / 1_073_741_824L) + "GB exceeds the 10GB limit");
        }
    }
}