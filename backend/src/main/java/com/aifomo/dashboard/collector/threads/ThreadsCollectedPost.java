package com.aifomo.dashboard.collector.threads;

import java.time.LocalDateTime;
import java.util.Objects;

public record ThreadsCollectedPost(
        String rawUrl,
        String rawContent,
        LocalDateTime publishedAt,
        LocalDateTime collectedAt
) {

    public ThreadsCollectedPost {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("rawUrl must not be blank");
        }
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("rawContent must not be blank");
        }
        Objects.requireNonNull(collectedAt, "collectedAt must not be null");
    }
}
