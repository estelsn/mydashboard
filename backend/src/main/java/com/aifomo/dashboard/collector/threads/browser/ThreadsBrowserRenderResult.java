package com.aifomo.dashboard.collector.threads.browser;

import java.util.Objects;

public record ThreadsBrowserRenderResult(
        String content,
        boolean loggedIn
) {

    public ThreadsBrowserRenderResult {
        Objects.requireNonNull(content, "content must not be null");
    }
}
