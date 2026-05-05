package com.aifomo.dashboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ManualThreadsCollectionRequest(
        @NotEmpty List<String> accountUrls,
        @Min(1) @Max(100) int maxPostsPerAccount,
        @Min(0) @Max(50) int maxScrollCount
) {
}
