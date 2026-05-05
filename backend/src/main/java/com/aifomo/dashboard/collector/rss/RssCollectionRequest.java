package com.aifomo.dashboard.collector.rss;

import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceType;

public record RssCollectionRequest(Source source, int maxItems) {

    public RssCollectionRequest {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (source.getSourceType() != SourceType.RSS_FEED && source.getSourceType() != SourceType.OFFICIAL_BLOG) {
            throw new IllegalArgumentException("source type must be RSS_FEED or OFFICIAL_BLOG");
        }
        if (maxItems < 1) {
            throw new IllegalArgumentException("maxItems must be greater than zero");
        }
    }
}
