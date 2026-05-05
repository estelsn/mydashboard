package com.aifomo.dashboard.collector.threads;

import com.aifomo.dashboard.domain.source.Source;

import java.util.List;
import java.util.Objects;

public record ThreadsCollectionResult(
        Source source,
        List<ThreadsCollectedPost> posts,
        List<String> warnings
) {

    public ThreadsCollectionResult {
        Objects.requireNonNull(source, "source must not be null");
        posts = List.copyOf(Objects.requireNonNull(posts, "posts must not be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings must not be null"));
    }

    public static ThreadsCollectionResult empty(Source source, String warning) {
        return new ThreadsCollectionResult(source, List.of(), List.of(warning));
    }
}
