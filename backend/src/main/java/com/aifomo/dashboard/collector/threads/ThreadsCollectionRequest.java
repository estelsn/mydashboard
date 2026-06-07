package com.aifomo.dashboard.collector.threads;

import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public record ThreadsCollectionRequest(
        Source source,
        int maxItems,
        int maxScrollCount,
        Long runId
) {

    public ThreadsCollectionRequest(Source source, int maxItems) {
        this(source, maxItems, 0, null);
    }

    public ThreadsCollectionRequest(Source source, int maxItems, int maxScrollCount) {
        this(source, maxItems, maxScrollCount, null);
    }

    public ThreadsCollectionRequest {
        Objects.requireNonNull(source, "source must not be null");
        if (source.getSourceType() != SourceType.THREADS_ACCOUNT) {
            throw new IllegalArgumentException("sourceType must be THREADS_ACCOUNT");
        }
        if (!isSupportedThreadsProfileUrl(source.getUrl())) {
            throw new IllegalArgumentException("source url must be a Threads profile URL");
        }
        if (maxItems < 1) {
            throw new IllegalArgumentException("maxItems must be greater than zero");
        }
        if (maxScrollCount < 0) {
            throw new IllegalArgumentException("maxScrollCount must not be negative");
        }
    }

    private static boolean isSupportedThreadsProfileUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();

            return "https".equalsIgnoreCase(scheme)
                    && ("www.threads.com".equalsIgnoreCase(host)
                    || "threads.com".equalsIgnoreCase(host)
                    || "www.threads.net".equalsIgnoreCase(host)
                    || "threads.net".equalsIgnoreCase(host))
                    && path != null
                    && path.matches("/@[^/]+/?");
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}
