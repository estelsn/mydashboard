package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.ThreadsCollectedPost;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionResult;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionStatus;
import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.collected.CollectedItemStatus;
import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.ImportanceLevel;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.util.ContentHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ThreadsCollectionPersistenceService {

    private static final int TITLE_LIMIT = 120;
    private static final int SUMMARY_LIMIT = 500;

    private final CollectionRunRepository collectionRunRepository;
    private final CollectedItemRepository collectedItemRepository;
    private final InfoItemRepository infoItemRepository;
    private final Clock clock;

    public ThreadsCollectionPersistenceService(
            CollectionRunRepository collectionRunRepository,
            CollectedItemRepository collectedItemRepository,
            InfoItemRepository infoItemRepository,
            Clock clock
    ) {
        this.collectionRunRepository = collectionRunRepository;
        this.collectedItemRepository = collectedItemRepository;
        this.infoItemRepository = infoItemRepository;
        this.clock = clock;
    }

    @Autowired
    public ThreadsCollectionPersistenceService(
            CollectionRunRepository collectionRunRepository,
            CollectedItemRepository collectedItemRepository,
            InfoItemRepository infoItemRepository
    ) {
        this(collectionRunRepository, collectedItemRepository, infoItemRepository, Clock.systemDefaultZone());
    }

    @Transactional
    public CollectionRun persist(ThreadsCollectionResult result) {
        LocalDateTime startedAt = LocalDateTime.now(clock);
        CollectionRun run = collectionRunRepository.save(new CollectionRun(
                CollectionRunStatus.RUNNING,
                1,
                0,
                0,
                0,
                startedAt,
                null,
                "Threads collection persistence started"
        ));

        if (result.status() != ThreadsCollectionStatus.SUCCESS && result.status() != ThreadsCollectionStatus.EMPTY_RESULT) {
            String failureReason = statusMessage(result);
            run.complete(
                    CollectionRunStatus.FAILED,
                    0,
                    1,
                    result.posts().size(),
                    0,
                    0,
                    result.posts().size(),
                    LocalDateTime.now(clock),
                    failureReason,
                    failureReason
            );
            return run;
        }

        PersistCounts counts = persistPosts(result.source(), result.posts());
        boolean failed = counts.failedCount() > 0;
        String failureReason = failed ? String.join("; ", counts.failures()) : null;
        run.complete(
                failed ? CollectionRunStatus.FAILED : CollectionRunStatus.SUCCEEDED,
                failed ? 0 : 1,
                failed ? 1 : 0,
                result.posts().size(),
                counts.createdCount(),
                counts.duplicateCount(),
                counts.failedCount(),
                LocalDateTime.now(clock),
                statusMessage(result, counts),
                failureReason
        );
        return run;
    }

    private PersistCounts persistPosts(Source source, List<ThreadsCollectedPost> posts) {
        int createdCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;
        List<String> failures = new ArrayList<>();

        for (ThreadsCollectedPost post : posts) {
            try {
                String normalizedContent = ContentHashUtil.normalize(post.rawContent());
                String contentHash = ContentHashUtil.sha256Normalized(post.rawContent());
                if (collectedItemRepository.findByContentHash(contentHash).isPresent()) {
                    duplicateCount++;
                    continue;
                }

                CollectedItem collectedItem = collectedItemRepository.save(new CollectedItem(
                        source,
                        post.rawUrl(),
                        post.rawContent(),
                        contentHash,
                        CollectedItemStatus.COLLECTED,
                        post.collectedAt()
                ));
                infoItemRepository.save(toInfoItem(source, collectedItem, post, normalizedContent));
                createdCount++;
            } catch (RuntimeException exception) {
                failedCount++;
                failures.add(post.rawUrl() + ": " + exception.getMessage());
            }
        }

        return new PersistCounts(createdCount, duplicateCount, failedCount, failures);
    }

    private InfoItem toInfoItem(Source source, CollectedItem collectedItem, ThreadsCollectedPost post, String normalizedContent) {
        return new InfoItem(
                source,
                collectedItem,
                truncate(normalizedContent, TITLE_LIMIT),
                truncate(normalizedContent, SUMMARY_LIMIT),
                post.rawUrl(),
                source.getCategory(),
                "[]",
                ImportanceLevel.MEDIUM,
                DecisionStatus.UNREVIEWED,
                false,
                false,
                false,
                null,
                false,
                post.collectedAt(),
                post.collectedAt()
        );
    }

    private String statusMessage(ThreadsCollectionResult result, PersistCounts counts) {
        String message = "Threads collection persisted: collected=%d, created=%d, duplicate=%d, failed=%d"
                .formatted(result.posts().size(), counts.createdCount(), counts.duplicateCount(), counts.failedCount());
        if (result.warnings().isEmpty()) {
            return message;
        }
        return message + "; warnings=" + String.join("; ", result.warnings());
    }

    private String statusMessage(ThreadsCollectionResult result) {
        String message = "Threads collection failed: status=" + result.status();
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
