package com.aifomo.dashboard.collector.threads.browser;

import com.aifomo.dashboard.collector.threads.ThreadsCollectedPost;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionRequest;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionResult;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionStatus;
import com.aifomo.dashboard.collector.threads.ThreadsCollector;
import com.aifomo.dashboard.collector.threads.ThreadsParsedPost;
import com.aifomo.dashboard.collector.threads.ThreadsPostParser;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionDescriptor;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionProvider;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionStatus;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@EnableConfigurationProperties(ThreadsBrowserCollectorProperties.class)
public class ThreadsBrowserCollector implements ThreadsCollector {

    private final BrowserSessionProvider sessionProvider;
    private final ThreadsBrowserPageClient pageClient;
    private final ThreadsPostParser parser;
    private final ThreadsBrowserCollectorProperties properties;
    private final Clock clock;

    @Autowired
    public ThreadsBrowserCollector(
            BrowserSessionProvider sessionProvider,
            ThreadsBrowserPageClient pageClient,
            ThreadsPostParser parser,
            ThreadsBrowserCollectorProperties properties
    ) {
        this(sessionProvider, pageClient, parser, properties, Clock.systemDefaultZone());
    }

    ThreadsBrowserCollector(
            BrowserSessionProvider sessionProvider,
            ThreadsBrowserPageClient pageClient,
            ThreadsPostParser parser,
            ThreadsBrowserCollectorProperties properties,
            Clock clock
    ) {
        this.sessionProvider = sessionProvider;
        this.pageClient = pageClient;
        this.parser = parser;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public ThreadsCollectionResult collect(ThreadsCollectionRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        BrowserSessionDescriptor session = sessionProvider.getSession();
        if (session.status() != BrowserSessionStatus.READY) {
            return sessionFailure(request, session);
        }

        int postLimit = postLimit(request);
        ThreadsBrowserPageSnapshot snapshot = pageClient.fetch(new ThreadsBrowserPageRequest(
                request.source().getUrl(),
                session.profileDirectory(),
                properties.isHeadless(),
                normalizedMaxScrollCount(),
                properties.getTimeout()
        ));
        if (snapshot.status() != ThreadsBrowserPageStatus.SUCCESS) {
            return ThreadsCollectionResult.failure(
                    request.source(),
                    toCollectionStatus(snapshot.status()),
                    defaultMessage(snapshot.message(), "Threads browser collection failed")
            );
        }

        List<ThreadsCollectedPost> posts = parser.parse(snapshot.rawContent()).stream()
                .limit(postLimit)
                .map(parsedPost -> toCollectedPost(request, parsedPost))
                .toList();
        if (posts.isEmpty()) {
            return ThreadsCollectionResult.failure(
                    request.source(),
                    ThreadsCollectionStatus.EMPTY_RESULT,
                    "Threads snapshot did not contain collectable posts"
            );
        }
        return new ThreadsCollectionResult(request.source(), ThreadsCollectionStatus.SUCCESS, posts, List.of());
    }

    private ThreadsCollectionResult sessionFailure(ThreadsCollectionRequest request, BrowserSessionDescriptor session) {
        ThreadsCollectionStatus status = switch (session.status()) {
            case LOGIN_REQUIRED, EXPIRED -> ThreadsCollectionStatus.LOGIN_REQUIRED;
            case NOT_CONFIGURED, ERROR -> ThreadsCollectionStatus.FAILED;
            case READY -> ThreadsCollectionStatus.SUCCESS;
        };
        return ThreadsCollectionResult.failure(
                request.source(),
                status,
                defaultMessage(session.message(), "Threads browser session is not ready")
        );
    }

    private ThreadsCollectedPost toCollectedPost(ThreadsCollectionRequest request, ThreadsParsedPost parsedPost) {
        String rawUrl = parsedPost.postUrl() == null ? request.source().getUrl() : parsedPost.postUrl();
        return new ThreadsCollectedPost(rawUrl, parsedPost.rawContent(), LocalDateTime.now(clock));
    }

    private int postLimit(ThreadsCollectionRequest request) {
        int configuredLimit = properties.getMaxPostsPerAccount();
        if (configuredLimit < 1) {
            configuredLimit = 1;
        }
        return Math.min(request.maxItems(), configuredLimit);
    }

    private int normalizedMaxScrollCount() {
        return Math.max(0, properties.getMaxScrollCount());
    }

    private static ThreadsCollectionStatus toCollectionStatus(ThreadsBrowserPageStatus status) {
        return switch (status) {
            case SUCCESS -> ThreadsCollectionStatus.SUCCESS;
            case LOGIN_REQUIRED -> ThreadsCollectionStatus.LOGIN_REQUIRED;
            case ACCESS_RESTRICTED -> ThreadsCollectionStatus.ACCESS_RESTRICTED;
            case EMPTY_RESULT -> ThreadsCollectionStatus.EMPTY_RESULT;
            case TIMEOUT -> ThreadsCollectionStatus.TIMEOUT;
            case FAILED -> ThreadsCollectionStatus.FAILED;
        };
    }

    private static String defaultMessage(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }
}
