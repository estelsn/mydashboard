package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.rss.RssCollectedItem;
import com.aifomo.dashboard.collector.rss.RssCollectionRequest;
import com.aifomo.dashboard.collector.rss.RssCollectionResult;
import com.aifomo.dashboard.collector.rss.RssCollectionStatus;
import com.aifomo.dashboard.collector.rss.RssCollector;
import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.collected.CollectedItemStatus;
import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.ImportanceLevel;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceType;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.repository.SourceRepository;
import com.aifomo.dashboard.util.ContentHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RssCollectionService {

    private static final int DEFAULT_MAX_ITEMS_PER_SOURCE = 20;
    private static final int TITLE_LIMIT = 120;
    private static final int SUMMARY_LIMIT = 500;
    private static final List<SourceType> COLLECTABLE_TYPES = List.of(SourceType.RSS_FEED, SourceType.OFFICIAL_BLOG);

    private final SourceRepository sourceRepository;
    private final CollectionRunRepository collectionRunRepository;
    private final CollectedItemRepository collectedItemRepository;
    private final InfoItemRepository infoItemRepository;
    private final RssCollector rssCollector;
    private final Clock clock;

    @Autowired
    public RssCollectionService(
            SourceRepository sourceRepository,
            CollectionRunRepository collectionRunRepository,
            CollectedItemRepository collectedItemRepository,
            InfoItemRepository infoItemRepository,
            RssCollector rssCollector
    ) {
        this(sourceRepository, collectionRunRepository, collectedItemRepository, infoItemRepository, rssCollector, Clock.systemDefaultZone());
    }

    RssCollectionService(
            SourceRepository sourceRepository,
            CollectionRunRepository collectionRunRepository,
            CollectedItemRepository collectedItemRepository,
            InfoItemRepository infoItemRepository,
            RssCollector rssCollector,
            Clock clock
    ) {
        this.sourceRepository = sourceRepository;
        this.collectionRunRepository = collectionRunRepository;
        this.collectedItemRepository = collectedItemRepository;
        this.infoItemRepository = infoItemRepository;
        this.rssCollector = rssCollector;
        this.clock = clock;
    }

    @Transactional
    public CollectionRun collectEnabledSources() {
        List<Source> sources = sourceRepository.findByEnabledTrueAndSourceTypeInOrderByPriorityAscIdAsc(COLLECTABLE_TYPES);
        LocalDateTime startedAt = LocalDateTime.now(clock);
        CollectionRun run = collectionRunRepository.save(new CollectionRun(
                CollectionRunStatus.RUNNING,
                sources.size(),
                0,
                0,
                0,
                startedAt,
                null,
                "RSS collection started"
        ));

        List<RssCollectionResult> results = sources.stream()
                .map(source -> collectSource(source, DEFAULT_MAX_ITEMS_PER_SOURCE))
                .toList();
        return persist(run, results);
    }

    private RssCollectionResult collectSource(Source source, int maxItems) {
        try {
            return rssCollector.collect(new RssCollectionRequest(source, maxItems));
        } catch (RuntimeException exception) {
            return RssCollectionResult.failure(source, RssCollectionStatus.FAILED, exception.getMessage());
        }
    }

    private CollectionRun persist(CollectionRun run, List<RssCollectionResult> results) {
        int successfulSourceCount = 0;
        int failedSourceCount = 0;
        int collectedItemCount = 0;
        int createdCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;
        List<String> failures = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        for (RssCollectionResult result : results) {
            collectedItemCount += result.items().size();

            if (result.status() != RssCollectionStatus.SUCCESS && result.status() != RssCollectionStatus.EMPTY_RESULT) {
                failedSourceCount++;
                String failureReason = statusMessage(result);
                failures.add(failureReason);
                messages.add(failureReason);
                continue;
            }

            PersistCounts counts = persistItems(result.source(), result.items());
            createdCount += counts.createdCount();
            duplicateCount += counts.duplicateCount();
            failedCount += counts.failedCount();
            if (counts.failedCount() > 0) {
                failedSourceCount++;
                failures.addAll(counts.failures());
            } else {
                successfulSourceCount++;
            }
            messages.add(statusMessage(result, counts));
        }

        boolean failed = failedSourceCount > 0 || failedCount > 0;
        run.complete(
                failed ? CollectionRunStatus.FAILED : CollectionRunStatus.SUCCEEDED,
                successfulSourceCount,
                failedSourceCount,
                collectedItemCount,
                createdCount,
                duplicateCount,
                failedCount,
                LocalDateTime.now(clock),
                String.join("; ", messages),
                failed ? String.join("; ", failures) : null
        );
        return run;
    }

    private PersistCounts persistItems(Source source, List<RssCollectedItem> items) {
        int createdCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;
        List<String> failures = new ArrayList<>();

        for (RssCollectedItem item : items) {
            try {
                String normalizedContent = ContentHashUtil.normalize(item.rawContent());
                String contentHash = ContentHashUtil.sha256Normalized(item.rawContent());
                if (collectedItemRepository.findByContentHash(contentHash).isPresent()) {
                    duplicateCount++;
                    continue;
                }

                CollectedItem collectedItem = collectedItemRepository.save(new CollectedItem(
                        source,
                        item.rawUrl(),
                        item.rawContent(),
                        contentHash,
                        CollectedItemStatus.COLLECTED,
                        item.collectedAt()
                ));
                infoItemRepository.save(toInfoItem(source, collectedItem, item, normalizedContent));
                source.setLastCollectedAt(item.collectedAt());
                createdCount++;
            } catch (RuntimeException exception) {
                failedCount++;
                failures.add(item.rawUrl() + ": " + exception.getMessage());
            }
        }

        return new PersistCounts(createdCount, duplicateCount, failedCount, failures);
    }

    private InfoItem toInfoItem(Source source, CollectedItem collectedItem, RssCollectedItem item, String normalizedContent) {
        String title = item.title().isBlank() ? normalizedContent : item.title();
        String summary = item.summary().isBlank() ? normalizedContent : item.summary();
        return new InfoItem(
                source,
                collectedItem,
                truncate(ContentHashUtil.normalize(title), TITLE_LIMIT),
                truncate(ContentHashUtil.normalize(summary), SUMMARY_LIMIT),
                item.rawUrl(),
                source.getCategory(),
                "[]",
                ImportanceLevel.MEDIUM,
                DecisionStatus.UNREVIEWED,
                false,
                false,
                false,
                null,
                false,
                item.publishedAt(),
                item.collectedAt()
        );
    }

    private String statusMessage(RssCollectionResult result, PersistCounts counts) {
        String message = "RSS collection persisted: collected=%d, created=%d, duplicate=%d, failed=%d"
                .formatted(result.items().size(), counts.createdCount(), counts.duplicateCount(), counts.failedCount());
        if (result.warnings().isEmpty()) {
            return message;
        }
        return message + "; warnings=" + String.join("; ", result.warnings());
    }

    private String statusMessage(RssCollectionResult result) {
        String message = "RSS collection failed: status=" + result.status();
        if (result.warnings().isEmpty()) {
            return message;
        }
        return message + "; warnings=" + String.join("; ", result.warnings());
    }

    private String truncate(String value, int limit) {
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }

    private record PersistCounts(int createdCount, int duplicateCount, int failedCount, List<String> failures) {
    }
}
