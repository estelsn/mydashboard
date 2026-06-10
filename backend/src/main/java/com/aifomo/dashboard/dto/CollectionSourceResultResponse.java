package com.aifomo.dashboard.dto;

import com.aifomo.dashboard.collector.threads.ThreadsCollectionStatus;
import com.aifomo.dashboard.domain.collection.CollectionSourceResult;

import java.time.LocalDateTime;

public record CollectionSourceResultResponse(
        Long id,
        Long sourceId,
        String sourceName,
        ThreadsCollectionStatus status,
        int collectedCount,
        int createdCount,
        int duplicateCount,
        int failedCount,
        String message,
        LocalDateTime createdAt
) {
    public static CollectionSourceResultResponse from(CollectionSourceResult result) {
        return new CollectionSourceResultResponse(
                result.getId(),
                result.getSource().getId(),
                result.getSource().getName(),
                result.getStatus(),
                result.getCollectedCount(),
                result.getCreatedCount(),
                result.getDuplicateCount(),
                result.getFailedCount(),
                result.getMessage(),
                result.getCreatedAt()
        );
    }
}
