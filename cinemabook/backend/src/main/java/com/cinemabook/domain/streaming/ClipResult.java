package com.cinemabook.domain.streaming;

public record ClipResult(
        String clipStorageKey,
        String clipUrl,
        int durationSeconds
) {}
