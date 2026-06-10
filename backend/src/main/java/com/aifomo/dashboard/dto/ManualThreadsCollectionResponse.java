package com.aifomo.dashboard.dto;

import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;

import java.util.List;

public record ManualThreadsCollectionResponse(
        Long runId,
        CollectionRunStatus status,
        int collectedCount,
        int createdCount,
        int duplicateCount,
        int failedCount,
        String failureReason,
        int requestedMaxPostsPerAccount,
        int appliedMaxPostsPerAccount,
        int requestedMaxScrollCount,
        int appliedMaxScrollCount,
        String safetyMessage,
        List<CollectionSourceResultResponse> sourceResults
) {
    public static ManualThreadsCollectionResponse from(
            CollectionRun run,
            int requestedMaxPostsPerAccount,
            int appliedMaxPostsPerAccount,
            int requestedMaxScrollCount,
            int appliedMaxScrollCount,
            String safetyMessage,
            List<CollectionSourceResultResponse> sourceResults
    ) {
        return new ManualThreadsCollectionResponse(
                run.getId(),
                run.getStatus(),
                run.getCollectedItemCount(),
                run.getCreatedCount(),
                run.getDuplicateCount(),
                run.getFailedCount(),
                run.getFailureReason(),
                requestedMaxPostsPerAccount,
                appliedMaxPostsPerAccount,
                requestedMaxScrollCount,
                appliedMaxScrollCount,
                safetyMessage,
                sourceResults
        );
    }
}
