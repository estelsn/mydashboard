package com.aifomo.dashboard.dto;

import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.ImportanceLevel;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.source.SourceCategory;

import java.time.LocalDateTime;

public record InfoItemResponse(
        Long id,
        Long sourceId,
        String sourceName,
        Long collectedItemId,
        String title,
        String summary,
        String originalUrl,
        SourceCategory category,
        String tags,
        ImportanceLevel importanceLevel,
        DecisionStatus decisionStatus,
        boolean manualOverride,
        boolean hidden,
        boolean deleted,
        Long duplicateOfId,
        boolean duplicate,
        LocalDateTime publishedAt,
        LocalDateTime collectedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        EvaluationResponse latestEvaluation
) {
    public static InfoItemResponse from(InfoItem infoItem, EvaluationResponse latestEvaluation) {
        return new InfoItemResponse(
                infoItem.getId(),
                infoItem.getSource().getId(),
                infoItem.getSource().getName(),
                infoItem.getCollectedItem().getId(),
                infoItem.getTitle(),
                infoItem.getSummary(),
                infoItem.getOriginalUrl(),
                infoItem.getCategory(),
                infoItem.getTags(),
                infoItem.getImportanceLevel(),
                infoItem.getDecisionStatus(),
                infoItem.isManualOverride(),
                infoItem.isHidden(),
                infoItem.isDeleted(),
                infoItem.getDuplicateOfId(),
                infoItem.isDuplicate(),
                infoItem.getPublishedAt(),
                infoItem.getCollectedAt(),
                infoItem.getCreatedAt(),
                infoItem.getUpdatedAt(),
                latestEvaluation
        );
    }
}
