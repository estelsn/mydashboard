package com.aifomo.dashboard.dto;

import com.aifomo.dashboard.domain.info.DecisionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDecisionStatusRequest(
        @NotNull DecisionStatus decisionStatus
) {
}
