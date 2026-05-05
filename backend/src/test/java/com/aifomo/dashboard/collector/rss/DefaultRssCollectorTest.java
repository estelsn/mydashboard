package com.aifomo.dashboard.collector.rss;

import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRssCollectorTest {

    @Test
    void collectsParsedFixtureItemsWithoutNetworkDependency() throws Exception {
        String xml = new String(
                getClass().getResourceAsStream("/rss-fixtures/openai-news.xml").readAllBytes(),
                StandardCharsets.UTF_8
        );
        DefaultRssCollector collector = new DefaultRssCollector(
                url -> xml,
                new RssFeedParser(),
                Clock.fixed(Instant.parse("2026-05-05T03:30:00Z"), ZoneId.of("Asia/Seoul"))
        );

        RssCollectionResult result = collector.collect(new RssCollectionRequest(source(), 1));

        assertThat(result.status()).isEqualTo(RssCollectionStatus.SUCCESS);
        assertThat(result.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.title()).isEqualTo("Codex improves dashboard automation");
                    assertThat(item.rawUrl()).isEqualTo("https://openai.com/news/codex-dashboard");
                    assertThat(item.rawContent()).contains("Codex improves dashboard automation");
                    assertThat(item.collectedAt()).isEqualTo(LocalDateTime.of(2026, 5, 5, 12, 30));
                });
    }

    private Source source() {
        return new Source(
                "OpenAI News RSS",
                SourceType.RSS_FEED,
                SourceCategory.COMPANY_OFFICIAL,
                "https://openai.com/news/rss.xml",
                "RSS source",
                true
        );
    }
}
