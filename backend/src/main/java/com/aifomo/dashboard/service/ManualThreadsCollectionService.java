package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.ThreadsCollectionRequest;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionResult;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualThreadsCollectionService {

    private final CollectionRunRepository collectionRunRepository;
    private final SourceRepository sourceRepository;
    private final ThreadsCollector threadsCollector;
    private final ThreadsCollectionPersistenceService persistenceService;

    public synchronized ManualThreadsCollectionResponse collect(ManualThreadsCollectionRequest request) {
        if (collectionRunRepository.existsByStatus(CollectionRunStatus.RUNNING)) {
            throw new DuplicateCollectionRunException("A collection run is already running");
        }

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
            for (String accountUrl : request.accountUrls()) {
                results.add(collectAccount(accountUrl, request));
            }
            return ManualThreadsCollectionResponse.from(persistenceService.persist(run, results));
        } catch (RuntimeException exception) {
            CollectionRun failedRun = persistenceService.fail(run, exception.getMessage());
            return ManualThreadsCollectionResponse.from(failedRun);
        }
    }

    private ThreadsCollectionResult collectAccount(String accountUrl, ManualThreadsCollectionRequest request) {
        Source source = resolveSource(accountUrl);
        try {
            return threadsCollector.collect(new ThreadsCollectionRequest(
                    source,
                    request.maxPostsPerAccount(),
                    request.maxScrollCount()
            ));
        } catch (RuntimeException exception) {
            return ThreadsCollectionResult.failure(source, ThreadsCollectionStatus.FAILED, exception.getMessage());
        }
    }

    private Source resolveSource(String accountUrl) {
        String normalizedUrl = normalizeUrl(accountUrl);
        Source candidate = new Source(
                sourceName(normalizedUrl),
                SourceType.THREADS,
                SourceCategory.NEWS,
                normalizedUrl,
                "Manual Threads collection source",
                true
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
}
