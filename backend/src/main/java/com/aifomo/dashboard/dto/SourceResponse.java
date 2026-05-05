package com.aifomo.dashboard.dto;

import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;

import java.time.LocalDateTime;

public record SourceResponse(
        Long id,
        String name,
        SourceType sourceType,
        SourceCategory category,
        String url,
        String description,
        boolean enabled,
        int priority,
        LocalDateTime lastCollectedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SourceResponse from(Source source) {
        return new SourceResponse(
                source.getId(),
                source.getName(),
                source.getSourceType(),
                source.getCategory(),
                source.getUrl(),
                source.getDescription(),
                source.isEnabled(),
                source.getPriority(),
                source.getLastCollectedAt(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }
}
