package com.aifomo.dashboard.collector.threads;

import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThreadsCollectionRequestTest {

    @Test
    void acceptsThreadsProfileSource() {
        Source source = new Source(
                "Choi OpenAI",
                SourceType.THREADS,
                SourceCategory.NEWS,
                "https://www.threads.com/@choi.openai",
                "Threads AI news curation",
                true
        );

        ThreadsCollectionRequest request = new ThreadsCollectionRequest(source, 10);

        assertThat(request.source()).isSameAs(source);
        assertThat(request.maxItems()).isEqualTo(10);
    }

    @Test
    void rejectsNonThreadsSourceType() {
        Source source = new Source(
                "OpenAI Blog",
                SourceType.OFFICIAL_BLOG,
                SourceCategory.COMPANY_OFFICIAL,
                "https://openai.com/blog",
                "Official blog",
                true
        );

        assertThatThrownBy(() -> new ThreadsCollectionRequest(source, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sourceType must be THREADS");
    }

    @Test
    void rejectsInvalidThreadsProfileUrl() {
        Source source = new Source(
                "Threads Post",
                SourceType.THREADS,
                SourceCategory.NEWS,
                "https://www.threads.com/@choi.openai/post/abc",
                "Post URL",
                true
        );

        assertThatThrownBy(() -> new ThreadsCollectionRequest(source, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("source url must be a Threads profile URL");
    }

    @Test
    void rejectsNonPositiveMaxItems() {
        Source source = new Source(
                "Choi OpenAI",
                SourceType.THREADS,
                SourceCategory.NEWS,
                "https://threads.com/@choi.openai",
                "Threads AI news curation",
                true
        );

        assertThatThrownBy(() -> new ThreadsCollectionRequest(source, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxItems must be greater than zero");
    }

    @Test
    void protectsResultListsFromExternalMutation() {
        Source source = new Source(
                "Choi OpenAI",
                SourceType.THREADS,
                SourceCategory.NEWS,
                "https://www.threads.com/@choi.openai",
                "Threads AI news curation",
                true
        );
        List<ThreadsCollectedPost> posts = new ArrayList<>();
        posts.add(new ThreadsCollectedPost(
                "https://www.threads.com/@choi.openai/post/seed-001",
                "Post content",
                LocalDateTime.of(2026, 5, 1, 10, 0)
        ));

        ThreadsCollectionResult result = new ThreadsCollectionResult(source, posts, List.of("limited public content"));
        posts.clear();

        assertThat(result.posts()).hasSize(1);
        assertThat(result.warnings()).containsExactly("limited public content");
        assertThatThrownBy(() -> result.posts().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
