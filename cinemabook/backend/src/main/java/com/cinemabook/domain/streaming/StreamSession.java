package com.cinemabook.domain.streaming;

import java.time.Instant;

public record StreamSession(
        String sessionId,
        String streamUrl,
        Instant expiresAt
) {}
