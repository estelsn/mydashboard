package com.aifomo.dashboard.collector.threads.browser;

import com.aifomo.dashboard.collector.threads.ThreadsCollectionRequest;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionStatus;
import com.aifomo.dashboard.collector.threads.ThreadsPostParser;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionDescriptor;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionProvider;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionStatus;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadsBrowserCollectorTest {

    @TempDir
    Path tempDir;

    @Test
    void fetchesSnapshotWithProfileDirectoryAndConfiguredLimits() {
        ThreadsBrowserCollectorProperties properties = new ThreadsBrowserCollectorProperties();
        properties.setHeadless(true);
        properties.setMaxScrollCount(2);
        properties.setMaxPostsPerAccount(1);
        CapturingPageClient pageClient = new CapturingPageClient(ThreadsBrowserPageSnapshot.success("""
                <article data-threads-post data-author="@choi.openai" data-url="https://www.threads.com/@choi.openai/post/1">
                  <div>First post</div>
                </article>
                <article data-threads-post data-author="@choi.openai" data-url="https://www.threads.com/@choi.openai/post/2">
                  <div>Second post</div>
                </article>
                """));

        var collector = collector(
                readySession(),
                pageClient,
                properties
        );

        var result = collector.collect(new ThreadsCollectionRequest(source(), 10));

        assertThat(result.status()).isEqualTo(ThreadsCollectionStatus.SUCCESS);
        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().getFirst().rawContent()).contains("First post");
        assertThat(result.posts().getFirst().collectedAt())
                .isEqualTo(LocalDateTime.of(2026, 5, 5, 12, 0));
        assertThat(pageClient.request.url()).isEqualTo("https://www.threads.com/@choi.openai");
        assertThat(pageClient.request.profileDirectory()).isEqualTo(tempDir);
        assertThat(pageClient.request.headless()).isTrue();
        assertThat(pageClient.request.maxScrollCount()).isEqualTo(2);
    }

    @Test
    void returnsLoginRequiredWhenSessionIsMissing() {
        var collector = collector(
                () -> new BrowserSessionDescriptor(
                        BrowserSessionStatus.LOGIN_REQUIRED,
                        tempDir,
                        "Open Chrome with the app profile directory and sign in to Threads"
                ),
                new CapturingPageClient(ThreadsBrowserPageSnapshot.success("<html></html>")),
                new ThreadsBrowserCollectorProperties()
        );

        var result = collector.collect(new ThreadsCollectionRequest(source(), 10));

        assertThat(result.status()).isEqualTo(ThreadsCollectionStatus.LOGIN_REQUIRED);
        assertThat(result.posts()).isEmpty();
        assertThat(result.warnings()).containsExactly("Open Chrome with the app profile directory and sign in to Threads");
    }

    @Test
    void mapsBrowserFailuresToExplicitCollectionStatuses() {
        var collector = collector(
                readySession(),
                new CapturingPageClient(ThreadsBrowserPageSnapshot.failure(
                        ThreadsBrowserPageStatus.ACCESS_RESTRICTED,
                        "Threads page access is restricted"
                )),
                new ThreadsBrowserCollectorProperties()
        );

        var result = collector.collect(new ThreadsCollectionRequest(source(), 10));

        assertThat(result.status()).isEqualTo(ThreadsCollectionStatus.ACCESS_RESTRICTED);
        assertThat(result.posts()).isEmpty();
        assertThat(result.warnings()).containsExactly("Threads page access is restricted");
    }

    @Test
    void returnsEmptyResultWhenParsedSnapshotHasNoPosts() {
        var collector = collector(
                readySession(),
                new CapturingPageClient(ThreadsBrowserPageSnapshot.success("<html><body>No posts</body></html>")),
                new ThreadsBrowserCollectorProperties()
        );

        var result = collector.collect(new ThreadsCollectionRequest(source(), 10));

        assertThat(result.status()).isEqualTo(ThreadsCollectionStatus.EMPTY_RESULT);
        assertThat(result.posts()).isEmpty();
        assertThat(result.warnings()).containsExactly("Threads snapshot did not contain collectable posts");
    }

    private ThreadsBrowserCollector collector(
            BrowserSessionProvider sessionProvider,
            ThreadsBrowserPageClient pageClient,
            ThreadsBrowserCollectorProperties properties
    ) {
        return new ThreadsBrowserCollector(
                sessionProvider,
                pageClient,
                new ThreadsPostParser(),
                properties,
                Clock.fixed(Instant.parse("2026-05-05T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    private BrowserSessionProvider readySession() {
        return () -> new BrowserSessionDescriptor(BrowserSessionStatus.READY, tempDir, "ready");
    }

    private static Source source() {
        return new Source(
                "Choi OpenAI",
                SourceType.THREADS,
                SourceCategory.NEWS,
                "https://www.threads.com/@choi.openai",
                "Threads AI news curation",
                true
        );
    }

    private static class CapturingPageClient implements ThreadsBrowserPageClient {

        private final ThreadsBrowserPageSnapshot snapshot;
        private ThreadsBrowserPageRequest request;

        private CapturingPageClient(ThreadsBrowserPageSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public ThreadsBrowserPageSnapshot fetch(ThreadsBrowserPageRequest request) {
            this.request = request;
            return snapshot;
        }
    }
}
