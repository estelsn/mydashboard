package com.aifomo.dashboard.dto;

import com.aifomo.dashboard.domain.evaluation.Evaluation;
import com.aifomo.dashboard.domain.evaluation.EvaluatorType;
import com.aifomo.dashboard.domain.info.DecisionStatus;

import java.time.LocalDateTime;

public record EvaluationResponse(
        Long id,
        DecisionStatus decisionStatus,
        String reason,
        double confidence,
        double relevanceScore,
        double actionabilityScore,
        double noveltyScore,
        EvaluatorType evaluatorType,
        String evaluatorVersion,
        LocalDateTime createdAt
) {
    public static EvaluationResponse from(Evaluation evaluation) {
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getDecisionStatus(),
                evaluation.getReason(),
                evaluation.getConfidence(),
                evaluation.getRelevanceScore(),
                evaluation.getActionabilityScore(),
                evaluation.getNoveltyScore(),
                evaluation.getEvaluatorType(),
                evaluation.getEvaluatorVersion(),
                evaluation.getCreatedAt()
        );
    }
}
