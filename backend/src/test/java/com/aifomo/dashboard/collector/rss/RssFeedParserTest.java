package com.aifomo.dashboard.collector.rss;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RssFeedParserTest {

    private final RssFeedParser parser = new RssFeedParser();

    @Test
    void parsesFixtureRssItems() throws Exception {
        String xml = new String(
                getClass().getResourceAsStream("/rss-fixtures/openai-news.xml").readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(parser.parse(xml))
                .hasSize(2)
                .first()
                .satisfies(item -> {
                    assertThat(item.title()).isEqualTo("Codex improves dashboard automation");
                    assertThat(item.link()).isEqualTo("https://openai.com/news/codex-dashboard");
                    assertThat(item.description()).isEqualTo("Codex can now automate recurring dashboard maintenance.");
                    assertThat(item.publishedAt()).isNotNull();
                });
    }
}
