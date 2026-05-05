package com.aifomo.dashboard.dto;

public record DashboardSummaryResponse(
        long totalCount,
        long visibleCount,
        long applyCount,
        long holdCount,
        long unreviewedCount,
        long ignoreCount,
        long archiveCandidateCount,
        long hiddenCount
) {
}
