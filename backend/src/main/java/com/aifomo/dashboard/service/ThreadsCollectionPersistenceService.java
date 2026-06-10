package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.ThreadsCollectedPost;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionResult;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionStatus;
import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.collected.CollectedItemStatus;
import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;
import com.aifomo.dashboard.domain.collection.CollectionSourceResult;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.ImportanceLevel;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import com.aifomo.dashboard.repository.CollectionSourceResultRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.util.ContentHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ThreadsCollectionPersistenceService {

    private static final int TITLE_LIMIT = 120;
    private static final int SUMMARY_LIMIT = 500;
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern RELATIVE_TIME_PATTERN = Pattern.compile(
            "\\b(\\d+\\s*(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|wk|wks|week|weeks)|yesterday)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern KOREAN_RELATIVE_TIME_PATTERN = Pattern.compile("(\\d+\\s*(초|분|시간|일|주)|어제)");
    private static final Pattern ABSOLUTE_DATE_PATTERN = Pattern.compile(
            "\\b(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}(?:[ t]\\d{1,2}:\\d{2}(?::\\d{2})?)?|[a-z]{3,9}\\s+\\d{1,2}|\\d{1,2}월\\s*\\d{1,2}일)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final CollectionRunRepository collectionRunRepository;
    private final CollectedItemRepository collectedItemRepository;
    private final InfoItemRepository infoItemRepository;
    private final CollectionSourceResultRepository collectionSourceResultRepository;
    private final Clock clock;

    public ThreadsCollectionPersistenceService(
            CollectionRunRepository collectionRunRepository,
            CollectedItemRepository collectedItemRepository,
            InfoItemRepository infoItemRepository,
            CollectionSourceResultRepository collectionSourceResultRepository,
            Clock clock
    ) {
        this.collectionRunRepository = collectionRunRepository;
        this.collectedItemRepository = collectedItemRepository;
        this.infoItemRepository = infoItemRepository;
        this.collectionSourceResultRepository = collectionSourceResultRepository;
        this.clock = clock;
    }

    @Autowired
    public ThreadsCollectionPersistenceService(
            CollectionRunRepository collectionRunRepository,
            CollectedItemRepository collectedItemRepository,
            InfoItemRepository infoItemRepository,
            CollectionSourceResultRepository collectionSourceResultRepository
    ) {
        this(
                collectionRunRepository,
                collectedItemRepository,
                infoItemRepository,
                collectionSourceResultRepository,
                Clock.systemDefaultZone()
        );
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
                "Threads 수집 저장을 시작했습니다."
        ));
        return persist(run, List.of(result));
    }

    @Transactional
    public CollectionRun persist(CollectionRun run, Collection<ThreadsCollectionResult> results) {
        int successfulSourceCount = 0;
        int failedSourceCount = 0;
        int collectedItemCount = 0;
        int createdCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;
        int cooldownSkippedCount = 0;
        List<String> failures = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        for (ThreadsCollectionResult result : results) {
            collectedItemCount += result.posts().size();

            if (result.status() != ThreadsCollectionStatus.SUCCESS
                    && result.status() != ThreadsCollectionStatus.EMPTY_RESULT
                    && result.status() != ThreadsCollectionStatus.COOLDOWN_SKIPPED) {
                failedSourceCount++;
                failedCount += result.posts().size();
                String failureReason = statusMessage(result);
                failures.add(failureReason);
                messages.add(failureReason);
                saveSourceResult(run, result, new PersistCounts(0, 0, result.posts().size(), List.of(failureReason)));
                continue;
            }

            if (result.status() == ThreadsCollectionStatus.COOLDOWN_SKIPPED) {
                cooldownSkippedCount++;
                saveSourceResult(run, result, new PersistCounts(0, 0, 0, List.of()));
                continue;
            }

            PersistCounts counts = persistPosts(result.source(), result.posts());
            log.info("Threads persistence result: runId={}, account={}, collectedCount={}, duplicateSkippedCount={}, createdCount={}, failedSaveCount={}",
                    run.getId(),
                    result.source().getUrl(),
                    result.posts().size(),
                    counts.duplicateCount(),
                    counts.createdCount(),
                    counts.failedCount());
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
            saveSourceResult(run, result, counts);
        }

        if (cooldownSkippedCount > 0) {
            messages.add("Threads 수집 건너뜀: 쿨다운 정책으로 %d개 소스를 건너뛰었습니다.".formatted(cooldownSkippedCount));
        }

        CollectionRunStatus finalStatus = finalStatus(
                successfulSourceCount,
                failedSourceCount,
                createdCount,
                failedCount
        );
        String failureReason = failures.isEmpty() ? null : String.join("; ", failures);
        run.complete(
                finalStatus,
                successfulSourceCount,
                failedSourceCount,
                collectedItemCount,
                createdCount,
                duplicateCount,
                failedCount,
                LocalDateTime.now(clock),
                String.join("; ", messages),
                failureReason
        );
        log.info("Threads run persistence complete: runId={}, status={}, collectedCount={}, createdCount={}, duplicateCount={}, failedCount={}, failedSourceCount={}",
                run.getId(),
                run.getStatus(),
                run.getCollectedItemCount(),
                run.getCreatedCount(),
                run.getDuplicateCount(),
                run.getFailedCount(),
                run.getFailedSourceCount());
        return run;
    }

    private CollectionRunStatus finalStatus(
            int successfulSourceCount,
            int failedSourceCount,
            int createdCount,
            int failedCount
    ) {
        if (failedSourceCount == 0 && failedCount == 0) {
            return CollectionRunStatus.SUCCEEDED;
        }
        if (successfulSourceCount > 0 || createdCount > 0) {
            return CollectionRunStatus.PARTIAL_SUCCESS;
        }
        return CollectionRunStatus.FAILED;
    }

    private void saveSourceResult(
            CollectionRun run,
            ThreadsCollectionResult result,
            PersistCounts counts
    ) {
        String message = result.warnings().isEmpty()
                ? null
                : String.join("; ", result.warnings());
        collectionSourceResultRepository.save(new CollectionSourceResult(
                run,
                result.source(),
                result.status(),
                result.posts().size(),
                counts.createdCount(),
                counts.duplicateCount(),
                counts.failedCount(),
                message
        ));
    }

    @Transactional
    public CollectionRun fail(CollectionRun run, String failureReason) {
        run.complete(
                CollectionRunStatus.FAILED,
                0,
                run.getTotalSourceCount(),
                0,
                0,
                0,
                0,
                LocalDateTime.now(clock),
                failureReason,
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
                String contentHash = stableContentHash(post);
                if (isDuplicate(post, contentHash)) {
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
                log.warn("Threads persistence failure: account={}, url={}, stage=save, message={}",
                        source.getUrl(),
                        post.rawUrl(),
                        exception.getMessage());
            }
        }

        return new PersistCounts(createdCount, duplicateCount, failedCount, failures);
    }

    private boolean isDuplicate(ThreadsCollectedPost post, String contentHash) {
        return collectedItemRepository.findByContentHash(contentHash).isPresent()
                || collectedItemRepository.findByRawUrl(post.rawUrl()).isPresent();
    }

    private String stableContentHash(ThreadsCollectedPost post) {
        return ContentHashUtil.sha256Normalized(canonicalDuplicateKey(post));
    }

    private String canonicalDuplicateKey(ThreadsCollectedPost post) {
        String withoutUrls = URL_PATTERN.matcher(post.rawContent()).replaceAll(" ");
        String withoutRelativeTime = RELATIVE_TIME_PATTERN.matcher(withoutUrls).replaceAll(" ");
        String withoutKoreanRelativeTime = KOREAN_RELATIVE_TIME_PATTERN.matcher(withoutRelativeTime).replaceAll(" ");
        String withoutAbsoluteDate = ABSOLUTE_DATE_PATTERN.matcher(withoutKoreanRelativeTime).replaceAll(" ");
        String normalized = ContentHashUtil.normalize(withoutAbsoluteDate);
        if (normalized.isBlank() && post.rawUrl() != null) {
            return post.rawUrl().toLowerCase(Locale.ROOT);
        }
        return normalized;
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
                post.publishedAt() == null ? post.collectedAt() : post.publishedAt(),
                post.collectedAt()
        );
    }

    private String statusMessage(ThreadsCollectionResult result, PersistCounts counts) {
        String message = "Threads collection persisted: collected=%d, created=%d, duplicate=%d, failed=%d"
                .formatted(result.posts().size(), counts.createdCount(), counts.duplicateCount(), counts.failedCount());
        message = "Threads 수집 저장 완료: 수집 %d, 생성 %d, 중복 %d, 실패 %d"
                .formatted(result.posts().size(), counts.createdCount(), counts.duplicateCount(), counts.failedCount());
        if (result.warnings().isEmpty()) {
            return message;
        }
        return message + "; 안내=" + String.join("; ", result.warnings());
    }

    private String statusMessage(ThreadsCollectionResult result) {
        String message = "Threads 수집 실패: 상태=" + result.status();
        if (result.warnings().isEmpty()) {
            return message;
        }
        return message + "; 안내=" + String.join("; ", result.warnings());
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
