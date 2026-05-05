package com.aifomo.dashboard.dto;

import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;

public record ManualThreadsCollectionResponse(
        Long runId,
        CollectionRunStatus status,
        int collectedCount,
        int createdCount,
        int duplicateCount,
        int failedCount,
        String failureReason
) {
    public static ManualThreadsCollectionResponse from(CollectionRun run) {
        return new ManualThreadsCollectionResponse(
                run.getId(),
                run.getStatus(),
                run.getCollectedItemCount(),
                run.getCreatedCount(),
                run.getDuplicateCount(),
                run.getFailedCount(),
                run.getFailureReason()
        );
    }
}
