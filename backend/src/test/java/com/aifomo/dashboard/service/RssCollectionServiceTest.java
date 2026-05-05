package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.rss.RssCollectedItem;
import com.aifomo.dashboard.collector.rss.RssCollectionRequest;
import com.aifomo.dashboard.collector.rss.RssCollectionResult;
import com.aifomo.dashboard.collector.rss.RssCollector;
import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RssCollectionServiceTest {

    private static final LocalDateTime COLLECTED_AT = LocalDateTime.of(2026, 5, 5, 12, 30);
    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 5, 5, 12, 0);

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private CollectionRunRepository collectionRunRepository;

    @Autowired
    private CollectedItemRepository collectedItemRepository;

    @Autowired
    private InfoItemRepository infoItemRepository;

    private RssCollectionService service;
    private Source rssSource;
    private Source officialBlogSource;

    @BeforeEach
    void setUp() {
        rssSource = sourceRepository.save(new Source(
                "OpenAI News RSS",
                SourceType.RSS_FEED,
                SourceCategory.COMPANY_OFFICIAL,
                "https://openai.com/news/rss.xml",
                "RSS source",
                true,
                10
        ));
        officialBlogSource = sourceRepository.save(new Source(
                "Anthropic News",
                SourceType.OFFICIAL_BLOG,
                SourceCategory.COMPANY_OFFICIAL,
                "https://www.anthropic.com/news",
                "Official blog source",
                true,
                20
        ));
        sourceRepository.save(new Source(
                "Disabled RSS",
                SourceType.RSS_FEED,
                SourceCategory.COMPANY_OFFICIAL,
                "https://example.com/rss.xml",
                "Disabled source",
                false,
                30
        ));
        sourceRepository.save(new Source(
                "Threads Source",
                SourceType.THREADS_ACCOUNT,
                SourceCategory.NEWS,
                "https://www.threads.com/@choi.openai",
                "Threads source",
                true,
                40
        ));
    }

    @Test
    void collectsEnabledRssAndOfficialSourcesAndPersistsItems() {
        service = serviceWith(request -> new RssCollectionResult(
                request.source(),
                List.of(item(request.source(), "Codex improves dashboard automation")),
                List.of()
        ));

        CollectionRun run = service.collectEnabledSources();

        assertThat(run.getStatus()).isEqualTo(CollectionRunStatus.SUCCEEDED);
        assertThat(run.getTotalSourceCount()).isEqualTo(2);
        assertThat(run.getSuccessfulSourceCount()).isEqualTo(2);
        assertThat(run.getFailedSourceCount()).isZero();
        assertThat(run.getCollectedItemCount()).isEqualTo(2);
        assertThat(run.getCreatedCount()).isEqualTo(2);
        assertThat(run.getDuplicateCount()).isZero();
        assertThat(run.getFailedCount()).isZero();
        assertThat(collectedItemRepository.findAll()).hasSize(2);
        assertThat(infoItemRepository.findAll()).hasSize(2)
                .allSatisfy(infoItem -> {
                    assertThat(infoItem.getDecisionStatus()).isEqualTo(DecisionStatus.UNREVIEWED);
                    assertThat(infoItem.getCategory()).isEqualTo(SourceCategory.COMPANY_OFFICIAL);
                    assertThat(infoItem.getPublishedAt()).isEqualTo(PUBLISHED_AT);
                    assertThat(infoItem.getCollectedAt()).isEqualTo(COLLECTED_AT);
                });
        assertThat(sourceRepository.findById(rssSource.getId()).orElseThrow().getLastCollectedAt()).isEqualTo(COLLECTED_AT);
        assertThat(sourceRepository.findById(officialBlogSource.getId()).orElseThrow().getLastCollectedAt()).isEqualTo(COLLECTED_AT);
    }

    @Test
    void preventsDuplicatePersistenceByContentHash() {
        service = serviceWith(request -> new RssCollectionResult(
                request.source(),
                List.of(item(request.source(), "Same announcement")),
                List.of()
        ));

        service.collectEnabledSources();
        CollectionRun duplicateRun = service.collectEnabledSources();

        assertThat(duplicateRun.getStatus()).isEqualTo(CollectionRunStatus.SUCCEEDED);
        assertThat(duplicateRun.getTotalSourceCount()).isEqualTo(2);
        assertThat(duplicateRun.getCollectedItemCount()).isEqualTo(2);
        assertThat(duplicateRun.getCreatedCount()).isZero();
        assertThat(duplicateRun.getDuplicateCount()).isEqualTo(2);
        assertThat(duplicateRun.getFailedCount()).isZero();
        assertThat(collectedItemRepository.findAll()).hasSize(2);
        assertThat(infoItemRepository.findAll()).hasSize(2);
    }

    private RssCollectionService serviceWith(RssCollector collector) {
        return new RssCollectionService(
                sourceRepository,
                collectionRunRepository,
                collectedItemRepository,
                infoItemRepository,
                collector,
                Clock.fixed(Instant.parse("2026-05-05T03:30:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    private RssCollectedItem item(Source source, String title) {
        String rawContent = title + "\n\n" + source.getName();
        return new RssCollectedItem(
                title,
                source.getName(),
                source.getUrl() + "/post",
                rawContent,
                PUBLISHED_AT,
                COLLECTED_AT
        );
    }

}
