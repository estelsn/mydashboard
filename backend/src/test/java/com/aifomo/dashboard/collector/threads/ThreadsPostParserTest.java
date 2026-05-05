package com.aifomo.dashboard.collector.threads;

import com.aifomo.dashboard.util.ContentHashUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadsPostParserTest {

    private final ThreadsPostParser parser = new ThreadsPostParser();

    @Test
    void parsesRenderedHtmlFixturePosts() throws IOException {
        String fixture = fixture("rendered-profile.html");

        var posts = parser.parse(fixture);

        assertThat(posts).hasSize(2);
        ThreadsParsedPost first = posts.getFirst();
        assertThat(first.authorIdentifier()).isEqualTo("@choi.openai");
        assertThat(first.body()).contains("OpenAI released a new model update today.");
        assertThat(first.postUrl()).isEqualTo("https://www.threads.com/@choi.openai/post/abc123");
        assertThat(first.displayTime()).isEqualTo("3h");
        assertThat(first.rawContent()).contains("@choi.openai", first.body(), first.postUrl(), "3h");
        assertThat(first.normalizedRawContent()).isEqualTo(ContentHashUtil.normalize(first.rawContent()));
        assertThat(first.contentHash()).isEqualTo(ContentHashUtil.sha256Normalized(first.rawContent()));
    }

    @Test
    void parsesJsonSnapshotPosts() throws IOException {
        String fixture = fixture("posts.json");

        var posts = parser.parse(fixture);

        assertThat(posts).hasSize(1);
        ThreadsParsedPost post = posts.getFirst();
        assertThat(post.authorIdentifier()).isEqualTo("@choi.openai");
        assertThat(post.body()).isEqualTo("JSON snapshot post body");
        assertThat(post.postUrl()).isEqualTo("https://www.threads.com/@choi.openai/post/json001");
        assertThat(post.displayTime()).isEqualTo("2026-05-01T10:15:00");
    }

    @Test
    void returnsEmptyListForEmptyOrBrokenFixtures() throws IOException {
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse(fixture("empty-profile.html"))).isEmpty();
        assertThat(parser.parse(fixture("broken-profile.html"))).isEmpty();
        assertThat(parser.parse("{ broken json")).isEmpty();
    }

    @Test
    void keepsPostWhenOptionalFieldsAreMissing() throws IOException {
        String fixture = fixture("missing-fields.html");

        var posts = parser.parse(fixture);

        assertThat(posts).hasSize(1);
        ThreadsParsedPost post = posts.getFirst();
        assertThat(post.authorIdentifier()).isNull();
        assertThat(post.postUrl()).isNull();
        assertThat(post.displayTime()).isNull();
        assertThat(post.body()).isEqualTo("Post body without author, url, or visible time.");
        assertThat(post.rawContent()).isEqualTo(post.body());
    }

    @Test
    void fixturesDoNotContainSensitiveSessionMaterial() throws IOException {
        for (String fixtureName : new String[] {
                "rendered-profile.html",
                "posts.json",
                "missing-fields.html",
                "empty-profile.html",
                "broken-profile.html"
        }) {
            String fixture = fixture(fixtureName).toLowerCase();

            assertThat(fixture)
                    .doesNotContain("cookie")
                    .doesNotContain("token")
                    .doesNotContain("password")
                    .doesNotContain("sessionid")
                    .doesNotContain("authorization");
        }
    }

    private static String fixture(String name) throws IOException {
        try (var inputStream = ThreadsPostParserTest.class.getResourceAsStream("/threads-fixtures/" + name)) {
            assertThat(inputStream).as(name).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
