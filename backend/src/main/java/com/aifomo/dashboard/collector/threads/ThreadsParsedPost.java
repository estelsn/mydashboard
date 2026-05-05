package com.aifomo.dashboard.collector.threads;

import com.aifomo.dashboard.util.ContentHashUtil;

import java.util.Objects;

public record ThreadsParsedPost(
        String authorIdentifier,
        String body,
        String postUrl,
        String displayTime,
        String rawContent,
        String normalizedRawContent,
        String contentHash
) {

    public ThreadsParsedPost(String authorIdentifier, String body, String postUrl, String displayTime, String rawContent) {
        this(
                blankToNull(authorIdentifier),
                requireNotBlank(body, "body must not be blank"),
                blankToNull(postUrl),
                blankToNull(displayTime),
                requireNotBlank(rawContent, "rawContent must not be blank"),
                ContentHashUtil.normalize(rawContent),
                ContentHashUtil.sha256Normalized(rawContent)
        );
    }

    public ThreadsParsedPost {
        body = requireNotBlank(body, "body must not be blank");
        rawContent = requireNotBlank(rawContent, "rawContent must not be blank");
        normalizedRawContent = requireNotBlank(normalizedRawContent, "normalizedRawContent must not be blank");
        contentHash = requireNotBlank(contentHash, "contentHash must not be blank");
        authorIdentifier = blankToNull(authorIdentifier);
        postUrl = blankToNull(postUrl);
        displayTime = blankToNull(displayTime);
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
