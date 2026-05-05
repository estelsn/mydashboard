package com.aifomo.dashboard.dto;

import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;

import java.time.LocalDateTime;

public record CollectionRunResponse(
        Long id,
        CollectionRunStatus status,
        int totalSourceCount,
        int successfulSourceCount,
        int failedSourceCount,
        int collectedItemCount,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String statusMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CollectionRunResponse from(CollectionRun collectionRun) {
        return new CollectionRunResponse(
                collectionRun.getId(),
                collectionRun.getStatus(),
                collectionRun.getTotalSourceCount(),
                collectionRun.getSuccessfulSourceCount(),
                collectionRun.getFailedSourceCount(),
                collectionRun.getCollectedItemCount(),
                collectionRun.getStartedAt(),
                collectionRun.getCompletedAt(),
                collectionRun.getStatusMessage(),
                collectionRun.getCreatedAt(),
                collectionRun.getUpdatedAt()
        );
    }
}
