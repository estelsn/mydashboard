package com.aifomo.dashboard.collector.threads;

import com.aifomo.dashboard.domain.source.Source;

import java.util.List;
import java.util.Objects;

public record ThreadsCollectionResult(
        Source source,
        ThreadsCollectionStatus status,
        List<ThreadsCollectedPost> posts,
        List<String> warnings
) {

    public ThreadsCollectionResult {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(status, "status must not be null");
        posts = List.copyOf(Objects.requireNonNull(posts, "posts must not be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings must not be null"));
    }

    public ThreadsCollectionResult(Source source, List<ThreadsCollectedPost> posts, List<String> warnings) {
        this(source, posts.isEmpty() ? ThreadsCollectionStatus.EMPTY_RESULT : ThreadsCollectionStatus.SUCCESS, posts, warnings);
    }

    public static ThreadsCollectionResult empty(Source source, String warning) {
        return new ThreadsCollectionResult(source, ThreadsCollectionStatus.EMPTY_RESULT, List.of(), List.of(warning));
    }

    public static ThreadsCollectionResult failure(Source source, ThreadsCollectionStatus status, String warning) {
        if (status == ThreadsCollectionStatus.SUCCESS) {
            throw new IllegalArgumentException("failure status must not be SUCCESS");
        }
        return new ThreadsCollectionResult(source, status, List.of(), List.of(warning));
    }
}
