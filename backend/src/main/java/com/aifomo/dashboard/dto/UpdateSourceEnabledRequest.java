package com.aifomo.dashboard.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSourceEnabledRequest(
        @NotNull Boolean enabled
) {
}
