package com.aifomo.dashboard.collector.rss;

import java.time.LocalDateTime;

public record RssCollectedItem(
        String title,
        String summary,
        String rawUrl,
        String rawContent,
        LocalDateTime publishedAt,
        LocalDateTime collectedAt
) {
    public RssCollectedItem {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("rawUrl must not be blank");
        }
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("rawContent must not be blank");
        }
        if (collectedAt == null) {
            throw new IllegalArgumentException("collectedAt must not be null");
        }
    }
}
