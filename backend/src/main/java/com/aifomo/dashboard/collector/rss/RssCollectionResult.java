package com.aifomo.dashboard.collector.rss;

import com.aifomo.dashboard.domain.source.Source;

import java.util.List;

public record RssCollectionResult(
        Source source,
        RssCollectionStatus status,
        List<RssCollectedItem> items,
        List<String> warnings
) {
    public RssCollectionResult {
        items = List.copyOf(items == null ? List.of() : items);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
    }

    public RssCollectionResult(Source source, List<RssCollectedItem> items, List<String> warnings) {
        this(source, items.isEmpty() ? RssCollectionStatus.EMPTY_RESULT : RssCollectionStatus.SUCCESS, items, warnings);
    }

    public static RssCollectionResult failure(Source source, RssCollectionStatus status, String warning) {
        if (status == RssCollectionStatus.SUCCESS || status == RssCollectionStatus.EMPTY_RESULT) {
            throw new IllegalArgumentException("failure status must not be successful");
        }
        return new RssCollectionResult(source, status, List.of(), List.of(warning));
    }
}
