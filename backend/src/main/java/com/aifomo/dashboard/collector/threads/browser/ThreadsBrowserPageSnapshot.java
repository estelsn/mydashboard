package com.aifomo.dashboard.collector.threads.browser;

import java.util.Objects;

public record ThreadsBrowserPageSnapshot(
        ThreadsBrowserPageStatus status,
        String rawContent,
        String message
) {

    public ThreadsBrowserPageSnapshot {
        Objects.requireNonNull(status, "status must not be null");
    }

    public static ThreadsBrowserPageSnapshot success(String rawContent) {
        return new ThreadsBrowserPageSnapshot(ThreadsBrowserPageStatus.SUCCESS, rawContent, null);
    }

    public static ThreadsBrowserPageSnapshot failure(ThreadsBrowserPageStatus status, String message) {
        if (status == ThreadsBrowserPageStatus.SUCCESS) {
            throw new IllegalArgumentException("failure status must not be SUCCESS");
        }
        return new ThreadsBrowserPageSnapshot(status, null, message);
    }
}
