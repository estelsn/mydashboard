package com.aifomo.dashboard.dto;

import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;

import java.time.LocalDateTime;
import java.util.List;

public record CollectionRunResponse(
        Long id,
        CollectionRunStatus status,
        int totalSourceCount,
        int successfulSourceCount,
        int failedSourceCount,
        int collectedItemCount,
        int createdCount,
        int duplicateCount,
        int failedCount,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String statusMessage,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CollectionSourceResultResponse> sourceResults
) {
    public static CollectionRunResponse from(
            CollectionRun collectionRun,
            List<CollectionSourceResultResponse> sourceResults
    ) {
        return new CollectionRunResponse(
                collectionRun.getId(),
                collectionRun.getStatus(),
                collectionRun.getTotalSourceCount(),
                collectionRun.getSuccessfulSourceCount(),
                collectionRun.getFailedSourceCount(),
                collectionRun.getCollectedItemCount(),
                collectionRun.getCreatedCount(),
                collectionRun.getDuplicateCount(),
                collectionRun.getFailedCount(),
                collectionRun.getStartedAt(),
                collectionRun.getCompletedAt(),
                collectionRun.getStatusMessage(),
                collectionRun.getFailureReason(),
                collectionRun.getCreatedAt(),
                collectionRun.getUpdatedAt(),
                sourceResults
        );
    }
}
