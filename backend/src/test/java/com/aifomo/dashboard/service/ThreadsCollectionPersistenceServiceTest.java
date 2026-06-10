package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.ThreadsCollectedPost;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionResult;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionStatus;
import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.collected.CollectedItemStatus;
import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import com.aifomo.dashboard.repository.CollectionSourceResultRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.repository.SourceRepository;
import com.aifomo.dashboard.util.ContentHashUtil;
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
class ThreadsCollectionPersistenceServiceTest {

    private static final LocalDateTime COLLECTED_AT = LocalDateTime.of(2026, 5, 5, 12, 0);

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private CollectionRunRepository collectionRunRepository;

    @Autowired
    private CollectedItemRepository collectedItemRepository;

    @Autowired
    private InfoItemRepository infoItemRepository;

    @Autowired
    private CollectionSourceResultRepository collectionSourceResultRepository;

    private ThreadsCollectionPersistenceService service;
    private Source source;

    @BeforeEach
    void setUp() {
        service = new ThreadsCollectionPersistenceService(
                collectionRunRepository,
                collectedItemRepository,
                infoItemRepository,
                collectionSourceResultRepository,
                Clock.fixed(Instant.parse("2026-05-05T03:30:00Z"), ZoneId.of("Asia/Seoul"))
        );
        source = sourceRepository.save(new Source(
                "Choi OpenAI",
                SourceType.THREADS_ACCOUNT,
                SourceCategory.NEWS,
                "https://www.threads.com/@choi.openai",
                "Threads AI news curation",
                true
        ));
    }

    @Test
    void savesCollectedItemAndInfoItemFromThreadsPost() {
        ThreadsCollectedPost post = new ThreadsCollectedPost(
                "https://www.threads.com/@choi.openai/post/1",
                "  OpenAI   coding agent update\nships today.  ",
                COLLECTED_AT.minusMinutes(30),
                COLLECTED_AT
        );

        CollectionRun run = service.persist(new ThreadsCollectionResult(source, List.of(post), List.of()));

        assertThat(run.getStatus()).isEqualTo(CollectionRunStatus.SUCCEEDED);
        assertThat(run.getCollectedItemCount()).isEqualTo(1);
        assertThat(run.getCreatedCount()).isEqualTo(1);
        assertThat(run.getDuplicateCount()).isZero();
        assertThat(run.getFailedCount()).isZero();

        List<CollectedItem> collectedItems = collectedItemRepository.findAll();
        assertThat(collectedItems).hasSize(1);
        CollectedItem collectedItem = collectedItems.getFirst();
        assertThat(collectedItem.getSource().getId()).isEqualTo(source.getId());
        assertThat(collectedItem.getRawUrl()).isEqualTo(post.rawUrl());
        assertThat(collectedItem.getRawContent()).isEqualTo(post.rawContent());
        assertThat(collectedItem.getContentHash())
                .isEqualTo(ContentHashUtil.sha256Normalized("OpenAI coding agent update ships today."));
        assertThat(collectedItem.getStatus()).isEqualTo(CollectedItemStatus.COLLECTED);

        assertThat(infoItemRepository.findAll()).singleElement()
                .satisfies(infoItem -> {
                    assertThat(infoItem.getCollectedItem().getId()).isEqualTo(collectedItem.getId());
                    assertThat(infoItem.getTitle()).isEqualTo("openai coding agent update ships today.");
                    assertThat(infoItem.getSummary()).isEqualTo("openai coding agent update ships today.");
                    assertThat(infoItem.getDecisionStatus()).isEqualTo(DecisionStatus.UNREVIEWED);
                    assertThat(infoItem.isHidden()).isFalse();
                    assertThat(infoItem.isDuplicate()).isFalse();
                    assertThat(infoItem.getPublishedAt()).isEqualTo(COLLECTED_AT.minusMinutes(30));
                    assertThat(infoItem.getCollectedAt()).isEqualTo(COLLECTED_AT);
                });
    }

    @Test
    void treatsExistingContentHashAsDuplicateWithoutCreatingNewInfoItem() {
        ThreadsCollectedPost first = new ThreadsCollectedPost(
                "https://www.threads.com/@choi.openai/post/1",
                "OpenAI coding agent update ships today.",
                COLLECTED_AT.minusMinutes(5),
                COLLECTED_AT
        );
        ThreadsCollectedPost duplicate = new ThreadsCollectedPost(
                "https://www.threads.com/@choi.openai/post/2",
                " openai   coding agent update ships today. ",
                COLLECTED_AT.minusMinutes(4),
                COLLECTED_AT.plusMinutes(1)
        );

        service.persist(new ThreadsCollectionResult(source, List.of(first), List.of()));
        CollectionRun duplicateRun = service.persist(new ThreadsCollectionResult(source, List.of(duplicate), List.of()));

        assertThat(duplicateRun.getStatus()).isEqualTo(CollectionRunStatus.SUCCEEDED);
        assertThat(duplicateRun.getCollectedItemCount()).isEqualTo(1);
        assertThat(duplicateRun.getCreatedCount()).isZero();
        assertThat(duplicateRun.getDuplicateCount()).isEqualTo(1);
        assertThat(duplicateRun.getFailedCount()).isZero();
        assertThat(collectedItemRepository.findAll()).hasSize(1);
        assertThat(infoItemRepository.findAll()).hasSize(1);
    }

    @Test
    void treatsSamePostUrlAsDuplicateEvenWhenRelativeTimeStringChanges() {
        ThreadsCollectedPost first = new ThreadsCollectedPost(
                "https://www.threads.com/@choi.openai/post/1",
                "@choi.openai\nSame post body\nhttps://www.threads.com/@choi.openai/post/1\n3h",
                COLLECTED_AT.minusHours(3),
                COLLECTED_AT
        );
        ThreadsCollectedPost duplicate = new ThreadsCollectedPost(
                "https://www.threads.com/@choi.openai/post/1",
                "@choi.openai\nSame post body\nhttps://www.threads.com/@choi.openai/post/1\n1d",
                COLLECTED_AT.minusDays(1),
                COLLECTED_AT.plusDays(1)
        );

        service.persist(new ThreadsCollectionResult(source, List.of(first), List.of()));
        CollectionRun duplicateRun = service.persist(new ThreadsCollectionResult(source, List.of(duplicate), List.of()));

        assertThat(duplicateRun.getCreatedCount()).isZero();
        assertThat(duplicateRun.getDuplicateCount()).isEqualTo(1);
        assertThat(collectedItemRepository.findAll()).hasSize(1);
        assertThat(infoItemRepository.findAll()).hasSize(1);
    }

    @Test
    void recordsCollectionFailureWithoutCreatingItems() {
        CollectionRun run = service.persist(ThreadsCollectionResult.failure(
                source,
                ThreadsCollectionStatus.LOGIN_REQUIRED,
                "Threads login required"
        ));

        assertThat(run.getStatus()).isEqualTo(CollectionRunStatus.FAILED);
        assertThat(run.getSuccessfulSourceCount()).isZero();
        assertThat(run.getFailedSourceCount()).isEqualTo(1);
        assertThat(run.getCollectedItemCount()).isZero();
        assertThat(run.getCreatedCount()).isZero();
        assertThat(run.getDuplicateCount()).isZero();
        assertThat(run.getFailedCount()).isZero();
        assertThat(run.getFailureReason()).contains("Threads login required");
        assertThat(collectedItemRepository.findAll()).isEmpty();
        assertThat(infoItemRepository.findAll()).isEmpty();
    }
}
