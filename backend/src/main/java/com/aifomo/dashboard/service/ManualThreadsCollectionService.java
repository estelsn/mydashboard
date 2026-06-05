package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.ThreadsCollectedPost;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionRequest;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionResult;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionProperties;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionSleeper;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionStatus;
import com.aifomo.dashboard.collector.threads.ThreadsCollector;
import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import com.aifomo.dashboard.dto.ManualThreadsCollectionRequest;
import com.aifomo.dashboard.dto.ManualThreadsCollectionResponse;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import com.aifomo.dashboard.repository.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(ThreadsCollectionProperties.class)
public class ManualThreadsCollectionService {
    private static final int RECENT_DAYS = 3;
    private static final List<SourceType> ENABLED_THREADS_TYPES = List.of(SourceType.THREADS_ACCOUNT);

    private final CollectionRunRepository collectionRunRepository;
    private final SourceRepository sourceRepository;
    private final ThreadsCollector threadsCollector;
    private final ThreadsCollectionPersistenceService persistenceService;
    private final ThreadsCollectionProperties properties;
    private final ThreadsCollectionSleeper sleeper;
    private final Clock clock;

    @Autowired
    public ManualThreadsCollectionService(
            CollectionRunRepository collectionRunRepository,
            SourceRepository sourceRepository,
            ThreadsCollector threadsCollector,
            ThreadsCollectionPersistenceService persistenceService,
            ThreadsCollectionProperties properties,
            ThreadsCollectionSleeper sleeper
    ) {
        this(collectionRunRepository, sourceRepository, threadsCollector, persistenceService, properties, sleeper, Clock.systemDefaultZone());
    }

    ManualThreadsCollectionService(
            CollectionRunRepository collectionRunRepository,
            SourceRepository sourceRepository,
            ThreadsCollector threadsCollector,
            ThreadsCollectionPersistenceService persistenceService,
            ThreadsCollectionProperties properties,
            ThreadsCollectionSleeper sleeper,
            Clock clock
    ) {
        this.collectionRunRepository = collectionRunRepository;
        this.sourceRepository = sourceRepository;
        this.threadsCollector = threadsCollector;
        this.persistenceService = persistenceService;
        this.properties = properties;
        this.sleeper = sleeper;
        this.clock = clock;
    }

    public synchronized ManualThreadsCollectionResponse collect(ManualThreadsCollectionRequest request) {
        if (collectionRunRepository.existsByStatus(CollectionRunStatus.RUNNING)) {
            throw new DuplicateCollectionRunException("A collection run is already running");
        }

        int requestedMaxPostsPerAccount = request.maxPostsPerAccount();
        int requestedMaxScrollCount = request.maxScrollCount();
        int appliedMaxPostsPerAccount = clamp(
                requestedMaxPostsPerAccount,
                properties.getDefaults().getMaxPostsPerAccount(),
                properties.getLimits().getMaxPostsPerAccount()
        );
        int appliedMaxScrollCount = clamp(
                requestedMaxScrollCount,
                0,
                properties.getLimits().getMaxScrollCount()
        );

        CollectionRun run = startRun(request.accountUrls().size(), "수동 Threads 수집을 시작했습니다.");

        try {
            List<ThreadsCollectionResult> results = collectAccounts(
                    request.accountUrls(),
                    appliedMaxPostsPerAccount,
                    appliedMaxScrollCount,
                    run
            );
            return ManualThreadsCollectionResponse.from(
                    persistenceService.persist(run, results),
                    requestedMaxPostsPerAccount,
                    appliedMaxPostsPerAccount,
                    requestedMaxScrollCount,
                    appliedMaxScrollCount,
                    safetyMessage(results)
            );
        } catch (RuntimeException exception) {
            CollectionRun failedRun = persistenceService.fail(run, exception.getMessage());
            return ManualThreadsCollectionResponse.from(
                    failedRun,
                    requestedMaxPostsPerAccount,
                    appliedMaxPostsPerAccount,
                    requestedMaxScrollCount,
                    appliedMaxScrollCount,
                    exception.getMessage()
            );
        }
    }

    public synchronized ManualThreadsCollectionResponse collectRecentFromEnabledSources() {
        if (collectionRunRepository.existsByStatus(CollectionRunStatus.RUNNING)) {
            throw new DuplicateCollectionRunException("A collection run is already running");
        }

        List<String> accountUrls = sourceRepository
                .findByEnabledTrueAndSourceTypeInOrderByPriorityAscIdAsc(ENABLED_THREADS_TYPES)
                .stream()
                .map(Source::getUrl)
                .toList();
        CollectionRun run = startRun(accountUrls.size(), "최근 3일 Threads 수집을 시작했습니다.");

        try {
            List<ThreadsCollectionResult> results = collectRecentAccounts(
                    accountUrls,
                    properties.getDefaults().getMaxPostsPerAccount(),
                    properties.getDefaults().getMaxScrollCount(),
                    LocalDateTime.now(clock).minusDays(RECENT_DAYS),
                    run
            );
            return ManualThreadsCollectionResponse.from(
                    persistenceService.persist(run, results),
                    properties.getDefaults().getMaxPostsPerAccount(),
                    properties.getDefaults().getMaxPostsPerAccount(),
                    properties.getDefaults().getMaxScrollCount(),
                    properties.getDefaults().getMaxScrollCount(),
                    recentCollectionSafetyMessage(results, accountUrls.size())
            );
        } catch (RuntimeException exception) {
            CollectionRun failedRun = persistenceService.fail(run, exception.getMessage());
            return ManualThreadsCollectionResponse.from(
                    failedRun,
                    properties.getDefaults().getMaxPostsPerAccount(),
                    properties.getDefaults().getMaxPostsPerAccount(),
                    properties.getDefaults().getMaxScrollCount(),
                    properties.getDefaults().getMaxScrollCount(),
                    exception.getMessage()
            );
        }
    }

    private CollectionRun startRun(int sourceCount, String statusMessage) {
        return collectionRunRepository.save(new CollectionRun(
                CollectionRunStatus.RUNNING,
                sourceCount,
                0,
                0,
                0,
                LocalDateTime.now(clock),
                null,
                statusMessage
        ));
    }

    private List<ThreadsCollectionResult> collectAccounts(
            List<String> accountUrls,
            int maxPostsPerAccount,
            int maxScrollCount
    ) {
        return collectAccounts(accountUrls, maxPostsPerAccount, maxScrollCount, null);
    }

    private List<ThreadsCollectionResult> collectAccounts(
            List<String> accountUrls,
            int maxPostsPerAccount,
            int maxScrollCount,
            CollectionRun run
    ) {
        List<ThreadsCollectionResult> results = new ArrayList<>();
        for (int index = 0; index < accountUrls.size(); index++) {
            if (index > 0) {
                sleeper.sleep(properties.getSafety().getDelayBetweenAccounts());
            }
            ThreadsCollectionResult result = collectAccount(
                    accountUrls.get(index),
                    maxPostsPerAccount,
                    maxScrollCount
            );
            results.add(result);
            if (run != null) {
                updateRunProgress(run, results);
            }
            if (properties.getSafety().getStopOnStatuses().contains(result.status())) {
                break;
            }
        }
        return results;
    }

    private List<ThreadsCollectionResult> collectRecentAccounts(
            List<String> accountUrls,
            int maxPostsPerAccount,
            int maxScrollCount,
            LocalDateTime cutoff
    ) {
        return collectRecentAccounts(accountUrls, maxPostsPerAccount, maxScrollCount, cutoff, null);
    }

    private List<ThreadsCollectionResult> collectRecentAccounts(
            List<String> accountUrls,
            int maxPostsPerAccount,
            int maxScrollCount,
            LocalDateTime cutoff,
            CollectionRun run
    ) {
        List<ThreadsCollectionResult> rawResults = collectAccounts(accountUrls, maxPostsPerAccount, maxScrollCount, run);
        List<ThreadsCollectionResult> filteredResults = new ArrayList<>();
        for (ThreadsCollectionResult rawResult : rawResults) {
            filteredResults.add(filterRecentPosts(rawResult, cutoff));
            if (run != null) {
                updateRunProgress(run, filteredResults);
            }
        }
        return filteredResults;
    }

    private ThreadsCollectionResult collectAccount(String accountUrl, int maxPostsPerAccount, int maxScrollCount) {
        Source source = resolveSource(accountUrl);
        ThreadsCollectionResult cooldownResult = cooldownResult(source);
        if (cooldownResult != null) {
            return cooldownResult;
        }
        try {
            ThreadsCollectionResult result = threadsCollector.collect(new ThreadsCollectionRequest(
                    source,
                    maxPostsPerAccount,
                    maxScrollCount
            ));
            source.setLastCollectedAt(LocalDateTime.now(clock));
            sourceRepository.save(source);
            return result;
        } catch (RuntimeException exception) {
            return ThreadsCollectionResult.failure(source, ThreadsCollectionStatus.FAILED, exception.getMessage());
        }
    }

    private ThreadsCollectionResult filterRecentPosts(ThreadsCollectionResult result, LocalDateTime cutoff) {
        if (result.status() != ThreadsCollectionStatus.SUCCESS && result.status() != ThreadsCollectionStatus.EMPTY_RESULT) {
            return result;
        }

        List<ThreadsCollectedPost> recentPosts = result.posts().stream()
                .filter(post -> post.publishedAt() != null && !post.publishedAt().isBefore(cutoff))
                .toList();
        if (recentPosts.isEmpty()) {
            return ThreadsCollectionResult.empty(
                    result.source(),
                    "최근 %d일 이내 게시물이 없어 저장하지 않았습니다.".formatted(RECENT_DAYS)
            );
        }

        List<String> warnings = new ArrayList<>(result.warnings());
        warnings.add("최근 %d일 필터 적용: %d건 중 %d건을 유지했습니다."
                .formatted(RECENT_DAYS, result.posts().size(), recentPosts.size()));
        return new ThreadsCollectionResult(result.source(), result.status(), recentPosts, warnings);
    }

    private ThreadsCollectionResult cooldownResult(Source source) {
        Duration interval = properties.getSafety().getMinSourceRecollectionInterval();
        LocalDateTime lastCollectedAt = source.getLastCollectedAt();
        if (lastCollectedAt == null || interval == null || interval.isZero() || interval.isNegative()) {
            return null;
        }

        LocalDateTime nextAllowedCollectionAt = lastCollectedAt.plus(interval);
        if (!LocalDateTime.now(clock).isBefore(nextAllowedCollectionAt)) {
            return null;
        }
        return ThreadsCollectionResult.failure(
                source,
                ThreadsCollectionStatus.COOLDOWN_SKIPPED,
                "최근에 수집한 소스라 건너뛰었습니다. 다음 수집 가능 시각: " + nextAllowedCollectionAt
        );
    }

    private Source resolveSource(String accountUrl) {
        String normalizedUrl = normalizeUrl(accountUrl);
        Source candidate = new Source(
                sourceName(normalizedUrl),
                SourceType.THREADS_ACCOUNT,
                SourceCategory.NEWS,
                normalizedUrl,
                "Manual Threads collection source",
                true,
                100
        );
        new ThreadsCollectionRequest(candidate, 1);
        return sourceRepository.findByUrl(normalizedUrl)
                .orElseGet(() -> sourceRepository.save(candidate));
    }

    private String normalizeUrl(String accountUrl) {
        if (accountUrl == null || accountUrl.isBlank()) {
            throw new IllegalArgumentException("accountUrls must contain only non-blank URLs");
        }
        return accountUrl.trim();
    }

    private String sourceName(String url) {
        int atIndex = url.indexOf("/@");
        if (atIndex < 0) {
            return "Manual Threads Source";
        }
        String handle = url.substring(atIndex + 2).replace("/", "");
        return handle.isBlank() ? "수동 Threads 소스" : handle;
    }

    private int clamp(int requestedValue, int defaultValue, int maxValue) {
        int safeDefault = Math.max(0, defaultValue);
        int safeMax = Math.max(safeDefault, maxValue);
        int normalized = requestedValue <= 0 ? safeDefault : requestedValue;
        return Math.min(normalized, safeMax);
    }

    private void updateRunProgress(CollectionRun run, List<ThreadsCollectionResult> results) {
        int processedSourceCount = results.size();
        int successfulSourceCount = 0;
        int failedSourceCount = 0;
        int collectedItemCount = 0;

        for (ThreadsCollectionResult result : results) {
            collectedItemCount += result.posts().size();
            if (result.status() == ThreadsCollectionStatus.SUCCESS || result.status() == ThreadsCollectionStatus.EMPTY_RESULT) {
                successfulSourceCount++;
            } else {
                failedSourceCount++;
            }
        }

        run.setStatus(CollectionRunStatus.RUNNING);
        run.setSuccessfulSourceCount(successfulSourceCount);
        run.setFailedSourceCount(failedSourceCount);
        run.setCollectedItemCount(collectedItemCount);
        run.setStatusMessage("진행 중: 전체 %d개 소스 중 %d개 처리 완료"
                .formatted(run.getTotalSourceCount(), processedSourceCount));
        collectionRunRepository.save(run);
    }

    private String safetyMessage(List<ThreadsCollectionResult> results) {
        for (ThreadsCollectionResult result : results) {
            if (properties.getSafety().getStopOnStatuses().contains(result.status())) {
                return switch (result.status()) {
                    case LOGIN_REQUIRED -> "Threads 로그인이 필요해 수집을 중단했습니다.";
                    case ACCESS_RESTRICTED -> "접근 제한이 감지되어 수집을 중단했습니다.";
                    case TIMEOUT -> "시간 초과로 수집을 중단했습니다.";
                    default -> "수집 상태가 " + result.status() + " 이어서 중단했습니다.";
                };
            }
            if (result.status() == ThreadsCollectionStatus.COOLDOWN_SKIPPED) {
                return result.warnings().isEmpty() ? "최근 수집된 소스라 건너뛰었습니다." : result.warnings().getFirst();
            }
        }
        return "안전 제한을 적용해 수집했습니다.";
    }

    private String recentCollectionSafetyMessage(List<ThreadsCollectionResult> results, int sourceCount) {
        if (sourceCount == 0) {
            return "활성화된 Threads 소스가 없습니다.";
        }

        String baseMessage = "%d초 간격으로 활성 Threads 소스를 순차 수집했고, 최근 %d일 필터를 적용했습니다."
                .formatted(properties.getSafety().getDelayBetweenAccounts().toSeconds(), RECENT_DAYS);
        String windowSummary = results.stream()
                .map(result -> result.warnings().stream()
                        .filter(warning -> warning.contains("최근"))
                        .collect(Collectors.joining(" ")))
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
        return List.of(baseMessage, safetyMessage(results), windowSummary).stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }
}
