package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.ThreadsCollectedPost;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionProperties;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionRequest;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionResult;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionSleeper;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionStatus;
import com.aifomo.dashboard.collector.threads.ThreadsCollector;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import com.aifomo.dashboard.dto.ManualThreadsCollectionRequest;
import com.aifomo.dashboard.dto.ManualThreadsCollectionResponse;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import com.aifomo.dashboard.repository.CollectionSourceResultRepository;
import com.aifomo.dashboard.repository.EvaluationRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ManualThreadsCollectionServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-05T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 5, 12, 0);

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

    @Autowired
    private EvaluationRepository evaluationRepository;

    private ThreadsCollectionProperties properties;
    private CapturingSleeper sleeper;

    @BeforeEach
    void setUp() {
        properties = new ThreadsCollectionProperties();
        properties.getDefaults().setMaxPostsPerAccount(3);
        properties.getDefaults().setMaxScrollCount(8);
        properties.getLimits().setMaxPostsPerAccount(5);
        properties.getLimits().setMaxScrollCount(12);
        properties.getSafety().setDelayBetweenAccounts(Duration.ofSeconds(5));
        properties.getSafety().setMinSourceRecollectionInterval(Duration.ofHours(1));
        sleeper = new CapturingSleeper();
    }

    @Test
    void clampsRequestedPostAndScrollLimitsBeforeCollecting() {
        CapturingThreadsCollector collector = collector(request -> success(request.source(), "clamped"));
        ManualThreadsCollectionResponse response = service(collector).collect(request(
                List.of("https://www.threads.com/@choi.openai"),
                100,
                50
        ));

        assertThat(collector.requests).singleElement()
                .satisfies(request -> {
                    assertThat(request.maxItems()).isEqualTo(5);
                    assertThat(request.maxScrollCount()).isEqualTo(12);
                });
        assertThat(response.requestedMaxPostsPerAccount()).isEqualTo(100);
        assertThat(response.appliedMaxPostsPerAccount()).isEqualTo(5);
        assertThat(response.requestedMaxScrollCount()).isEqualTo(50);
        assertThat(response.appliedMaxScrollCount()).isEqualTo(12);
    }

    @Test
    void delaysBetweenAccountsWithoutSleepingInTest() {
        CapturingThreadsCollector collector = collector(request -> success(request.source(), request.source().getName()));

        service(collector).collect(request(List.of(
                "https://www.threads.com/@first",
                "https://www.threads.com/@second"
        ), 3, 1));

        assertThat(collector.requests).hasSize(2);
        assertThat(sleeper.durations).containsExactly(Duration.ofSeconds(5));
    }

    @Test
    void stopsImmediatelyWhenLoginRequired() {
        assertStopsOnRiskStatus(
                ThreadsCollectionStatus.LOGIN_REQUIRED,
                "공용 Threads 세션을 사용할 수 없어 전체 수집을 중단했습니다."
        );
    }

    @Test
    void continuesAfterAccessRestricted() {
        assertContinuesAfterSourceFailure(ThreadsCollectionStatus.ACCESS_RESTRICTED);
    }

    @Test
    void continuesAfterTimeout() {
        assertContinuesAfterSourceFailure(ThreadsCollectionStatus.TIMEOUT);
    }

    @Test
    void collectsOnlyPostsFromLastThreeDaysAcrossEnabledSources() {
        sourceRepository.save(new Source(
                "First Threads",
                SourceType.THREADS_ACCOUNT,
                SourceCategory.NEWS,
                "https://www.threads.com/@first",
                "first",
                true,
                10
        ));
        sourceRepository.save(new Source(
                "Second Threads",
                SourceType.THREADS_ACCOUNT,
                SourceCategory.NEWS,
                "https://www.threads.com/@second",
                "second",
                true,
                20
        ));
        CapturingThreadsCollector collector = collector(request -> new ThreadsCollectionResult(
                request.source(),
                List.of(
                        new ThreadsCollectedPost(
                                request.source().getUrl() + "/post/new",
                                request.source().getName() + " new",
                                NOW.minusDays(1),
                                NOW
                        ),
                        new ThreadsCollectedPost(request.source().getUrl() + "/post/old", "old", NOW.minusDays(4), NOW),
                        new ThreadsCollectedPost(request.source().getUrl() + "/post/unknown", "unknown", null, NOW)
                ),
                List.of()
        ));

        ManualThreadsCollectionResponse response = service(collector).collectRecentFromEnabledSources();

        assertThat(response.status().name()).isEqualTo("SUCCEEDED");
        assertThat(response.collectedCount()).isEqualTo(2);
        assertThat(response.createdCount()).isEqualTo(2);
        assertThat(response.safetyMessage()).contains("최근 3일 필터");
        assertThat(infoItemRepository.findAll()).allSatisfy(infoItem ->
                assertThat(infoItem.getPublishedAt()).isAfterOrEqualTo(NOW.minusDays(3)));
        assertThat(sleeper.durations).containsExactly(Duration.ofSeconds(5));
    }

    @Test
    void evaluatesNewItemsImmediatelyAfterCollection() {
        CapturingThreadsCollector collector = collector(request -> success(
                request.source(),
                "Codex OAuth integration setup guide for developer automation workflows."
        ));

        ManualThreadsCollectionResponse response = service(collector).collect(request(
                List.of("https://www.threads.com/@choi.openai"),
                3,
                1
        ));

        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(infoItemRepository.findAll()).singleElement().satisfies(infoItem ->
                assertThat(infoItem.getDecisionStatus()).isEqualTo(com.aifomo.dashboard.domain.info.DecisionStatus.APPLY));
        assertThat(evaluationRepository.findAll()).singleElement().satisfies(evaluation ->
                assertThat(evaluation.getEvaluatorVersion()).isEqualTo(RuleBasedEvaluator.EVALUATOR_VERSION));
    }

    @Test
    void skipsRecentlyCollectedSourceByCooldownPolicy() {
        Source source = sourceRepository.save(new Source(
                "Recent Threads",
                SourceType.THREADS_ACCOUNT,
                SourceCategory.NEWS,
                "https://www.threads.com/@recent",
                "recent",
                true
        ));
        new ThreadsCollectionPersistenceService(
                collectionRunRepository,
                collectedItemRepository,
                infoItemRepository,
                collectionSourceResultRepository,
                CLOCK
        ).persist(success(source, "existing"));
        source.setLastCollectedAt(NOW.minusMinutes(30));
        sourceRepository.save(source);
        CapturingThreadsCollector collector = collector(request -> success(request.source(), "should not run"));

        ManualThreadsCollectionResponse response = service(collector).collect(request(
                List.of("https://www.threads.com/@recent"),
                3,
                1
        ));

        assertThat(collector.requests).isEmpty();
        assertThat(response.status().name()).isEqualTo("SUCCEEDED");
        assertThat(response.safetyMessage()).isEqualTo("쿨다운 정책으로 1개 소스를 건너뛰었습니다.");
        assertThat(collectedItemRepository.findAll()).hasSize(1);
        assertThat(infoItemRepository.findAll()).hasSize(1);
    }

    private void assertStopsOnRiskStatus(ThreadsCollectionStatus status, String safetyMessage) {
        CapturingThreadsCollector collector = collector(request -> ThreadsCollectionResult.failure(
                request.source(),
                status,
                status.name()
        ));

        ManualThreadsCollectionResponse response = service(collector).collect(request(List.of(
                "https://www.threads.com/@first",
                "https://www.threads.com/@second"
        ), 3, 1));

        assertThat(collector.requests).hasSize(1);
        assertThat(sleeper.durations).isEmpty();
        assertThat(response.status().name()).isEqualTo("FAILED");
        assertThat(response.failureReason()).contains("상태=" + status);
        assertThat(response.safetyMessage()).isEqualTo(safetyMessage);
    }

    private void assertContinuesAfterSourceFailure(ThreadsCollectionStatus status) {
        CapturingThreadsCollector collector = collector(request -> {
            if (request.source().getUrl().endsWith("@first")) {
                return ThreadsCollectionResult.failure(request.source(), status, status.name());
            }
            return success(request.source(), "second source success");
        });

        ManualThreadsCollectionResponse response = service(collector).collect(request(List.of(
                "https://www.threads.com/@first",
                "https://www.threads.com/@second"
        ), 3, 1));

        assertThat(collector.requests).hasSize(2);
        assertThat(response.status().name()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.sourceResults()).hasSize(2);
        assertThat(response.safetyMessage()).contains("나머지 소스 수집을 계속");
    }

    private ManualThreadsCollectionService service(ThreadsCollector collector) {
        return new ManualThreadsCollectionService(
                collectionRunRepository,
                sourceRepository,
                collectedItemRepository,
                collectionSourceResultRepository,
                collector,
                new ThreadsCollectionPersistenceService(
                        collectionRunRepository,
                        collectedItemRepository,
                        infoItemRepository,
                        collectionSourceResultRepository,
                        CLOCK
                ),
                new EvaluationService(
                        new RuleBasedEvaluator(),
                        infoItemRepository,
                        evaluationRepository
                ),
                properties,
                sleeper,
                CLOCK
        );
    }

    @Test
    void ignoresCooldownWhenSourceHasNoCollectedItems() {
        Source source = sourceRepository.save(new Source(
                "Recent Threads",
                SourceType.THREADS_ACCOUNT,
                SourceCategory.NEWS,
                "https://www.threads.com/@recent",
                "recent",
                true
        ));
        source.setLastCollectedAt(NOW.minusMinutes(30));
        sourceRepository.save(source);
        CapturingThreadsCollector collector = collector(request -> success(request.source(), "fresh"));

        ManualThreadsCollectionResponse response = service(collector).collect(request(
                List.of("https://www.threads.com/@recent"),
                3,
                1
        ));

        assertThat(collector.requests).hasSize(1);
        assertThat(response.createdCount()).isEqualTo(1);
    }

    private CapturingThreadsCollector collector(Function<ThreadsCollectionRequest, ThreadsCollectionResult> handler) {
        return new CapturingThreadsCollector(handler);
    }

    private ManualThreadsCollectionRequest request(List<String> accountUrls, int maxPostsPerAccount, int maxScrollCount) {
        return new ManualThreadsCollectionRequest(accountUrls, maxPostsPerAccount, maxScrollCount);
    }

    private ThreadsCollectionResult success(Source source, String content) {
        return new ThreadsCollectionResult(
                source,
                List.of(new ThreadsCollectedPost(source.getUrl() + "/post/1", content, NOW, NOW)),
                List.of()
        );
    }

    private static class CapturingThreadsCollector implements ThreadsCollector {

        private final Function<ThreadsCollectionRequest, ThreadsCollectionResult> handler;
        private final List<ThreadsCollectionRequest> requests = new ArrayList<>();

        private CapturingThreadsCollector(Function<ThreadsCollectionRequest, ThreadsCollectionResult> handler) {
            this.handler = handler;
        }

        @Override
        public ThreadsCollectionResult collect(ThreadsCollectionRequest request) {
            requests.add(request);
            return handler.apply(request);
        }
    }

    private static class CapturingSleeper implements ThreadsCollectionSleeper {

        private final List<Duration> durations = new ArrayList<>();

        @Override
        public void sleep(Duration duration) {
            durations.add(duration);
        }
    }
}
