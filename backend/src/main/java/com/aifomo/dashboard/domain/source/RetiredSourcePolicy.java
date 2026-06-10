package com.aifomo.dashboard.domain.source;

import java.util.Locale;
import java.util.Set;

public final class RetiredSourcePolicy {

    private static final Set<String> RETIRED_URLS = Set.of(
            "https://www.threads.com/@appcast",
            "https://www.threads.com/@ethancl",
            "https://www.threads.com/@specal1849",
            "https://www.threads.com/@xazinga",
            "https://www.threads.com/@apple_tea_94"
    );

    private RetiredSourcePolicy() {
    }

    public static boolean isRetired(Source source) {
        return source != null && isRetired(source.getUrl());
    }

    public static boolean isRetired(String url) {
        return url != null && RETIRED_URLS.contains(url.trim().toLowerCase(Locale.ROOT));
    }
}
