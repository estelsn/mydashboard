package com.aifomo.dashboard.service;

import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.ImportanceLevel;

public record RuleBasedEvaluationResult(
        DecisionStatus decisionStatus,
        ImportanceLevel importanceLevel,
        String reason,
        double confidence,
        double relevanceScore,
        double actionabilityScore,
        double noveltyScore
) {
}
