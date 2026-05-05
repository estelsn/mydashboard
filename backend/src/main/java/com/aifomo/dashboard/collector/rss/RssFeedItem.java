package com.aifomo.dashboard.collector.rss;

import java.time.LocalDateTime;

public record RssFeedItem(
        String title,
        String link,
        String description,
        LocalDateTime publishedAt
) {
}
