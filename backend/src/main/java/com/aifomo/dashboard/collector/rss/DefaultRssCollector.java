package com.aifomo.dashboard.collector.rss;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DefaultRssCollector implements RssCollector {

    private final RssFeedClient rssFeedClient;
    private final RssFeedParser rssFeedParser;
    private final Clock clock;

    @Autowired
    public DefaultRssCollector(RssFeedClient rssFeedClient, RssFeedParser rssFeedParser) {
        this(rssFeedClient, rssFeedParser, Clock.systemDefaultZone());
    }

    DefaultRssCollector(RssFeedClient rssFeedClient, RssFeedParser rssFeedParser, Clock clock) {
        this.rssFeedClient = rssFeedClient;
        this.rssFeedParser = rssFeedParser;
        this.clock = clock;
    }

    @Override
    public RssCollectionResult collect(RssCollectionRequest request) {
        String xml;
        try {
            xml = rssFeedClient.fetch(request.source().getUrl());
        } catch (IOException exception) {
            return RssCollectionResult.failure(request.source(), RssCollectionStatus.FETCH_FAILED, exception.getMessage());
        }

        List<RssFeedItem> feedItems;
        try {
            feedItems = rssFeedParser.parse(xml);
        } catch (RuntimeException exception) {
            return RssCollectionResult.failure(request.source(), RssCollectionStatus.PARSE_FAILED, exception.getMessage());
        }

        LocalDateTime collectedAt = LocalDateTime.now(clock);
        List<RssCollectedItem> items = feedItems.stream()
                .limit(request.maxItems())
                .map(item -> toCollectedItem(item, collectedAt))
                .toList();
        return new RssCollectionResult(request.source(), items, List.of());
    }

    private RssCollectedItem toCollectedItem(RssFeedItem item, LocalDateTime collectedAt) {
        String summary = item.description() == null ? "" : item.description();
        String rawContent = (item.title() + "\n\n" + summary).trim();
        return new RssCollectedItem(
                item.title(),
                summary.isBlank() ? item.title() : summary,
                item.link(),
                rawContent,
                item.publishedAt(),
                collectedAt
        );
    }
}
