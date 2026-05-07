package com.aifomo.dashboard.service;

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

@Service
@EnableConfigurationProperties(ThreadsCollectionProperties.class)
public class ManualThreadsCollectionService {

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
                properties.getDefaults().getMaxScrollCount(),
                properties.getLimits().getMaxScrollCount()
        );

        CollectionRun run = collectionRunRepository.save(new CollectionRun(
                CollectionRunStatus.RUNNING,
                request.accountUrls().size(),
                0,
                0,
                0,
                LocalDateTime.now(),
                null,
                "Manual Threads collection started"
        ));

        try {
            List<ThreadsCollectionResult> results = new ArrayList<>();
            for (int index = 0; index < request.accountUrls().size(); index++) {
                if (index > 0) {
                    sleeper.sleep(properties.getSafety().getDelayBetweenAccounts());
                }
                ThreadsCollectionResult result = collectAccount(
                        request.accountUrls().get(index),
                        appliedMaxPostsPerAccount,
                        appliedMaxScrollCount
                );
                results.add(result);
                if (properties.getSafety().getStopOnStatuses().contains(result.status())) {
                    break;
                }
            }
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
                "COOLDOWN_SKIPPED: Source was collected recently. Next allowed collection at "
                        + nextAllowedCollectionAt
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
        return handle.isBlank() ? "Manual Threads Source" : handle;
    }

    private int clamp(int requestedValue, int defaultValue, int maxValue) {
        int safeDefault = Math.max(0, defaultValue);
        int safeMax = Math.max(safeDefault, maxValue);
        int normalized = requestedValue <= 0 ? safeDefault : requestedValue;
        return Math.min(normalized, safeMax);
    }

    private String safetyMessage(List<ThreadsCollectionResult> results) {
        for (ThreadsCollectionResult result : results) {
            if (properties.getSafety().getStopOnStatuses().contains(result.status())) {
                return switch (result.status()) {
                    case LOGIN_REQUIRED -> "Stopped because Threads session status was LOGIN_REQUIRED.";
                    case ACCESS_RESTRICTED -> "Stopped because Threads access appeared restricted.";
                    case TIMEOUT -> "Stopped because Threads collection timed out.";
                    default -> "Stopped because Threads collection returned status " + result.status() + ".";
                };
            }
            if (result.status() == ThreadsCollectionStatus.COOLDOWN_SKIPPED) {
                return result.warnings().isEmpty() ? "COOLDOWN_SKIPPED" : result.warnings().getFirst();
            }
        }
        return "Threads collection safety limits applied.";
    }
}
