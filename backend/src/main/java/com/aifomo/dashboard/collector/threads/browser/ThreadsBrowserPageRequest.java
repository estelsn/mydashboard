package com.aifomo.dashboard.collector.threads.browser;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public record ThreadsBrowserPageRequest(
        String url,
        Path profileDirectory,
        boolean headless,
        int maxScrollCount,
        Duration timeout
) {

    public ThreadsBrowserPageRequest {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        Objects.requireNonNull(profileDirectory, "profileDirectory must not be null");
        if (maxScrollCount < 0) {
            throw new IllegalArgumentException("maxScrollCount must not be negative");
        }
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}
