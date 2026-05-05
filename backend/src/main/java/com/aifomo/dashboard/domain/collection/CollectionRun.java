package com.aifomo.dashboard.domain.collection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollectionRunStatus status;

    @Column(nullable = false)
    private int totalSourceCount;

    @Column(nullable = false)
    private int successfulSourceCount;

    @Column(nullable = false)
    private int failedSourceCount;

    @Column(nullable = false)
    private int collectedItemCount;

    @Column(nullable = false)
    private int createdCount;

    @Column(nullable = false)
    private int duplicateCount;

    @Column(nullable = false)
    private int failedCount;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Lob
    private String statusMessage;

    @Lob
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public CollectionRun(
            CollectionRunStatus status,
            int totalSourceCount,
            int successfulSourceCount,
            int failedSourceCount,
            int collectedItemCount,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            String statusMessage
    ) {
        this.status = status;
        this.totalSourceCount = totalSourceCount;
        this.successfulSourceCount = successfulSourceCount;
        this.failedSourceCount = failedSourceCount;
        this.collectedItemCount = collectedItemCount;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.statusMessage = statusMessage;
    }

    public void complete(
            CollectionRunStatus status,
            int successfulSourceCount,
            int failedSourceCount,
            int collectedItemCount,
            int createdCount,
            int duplicateCount,
            int failedCount,
            LocalDateTime completedAt,
            String statusMessage,
            String failureReason
    ) {
        this.status = status;
        this.successfulSourceCount = successfulSourceCount;
        this.failedSourceCount = failedSourceCount;
        this.collectedItemCount = collectedItemCount;
        this.createdCount = createdCount;
        this.duplicateCount = duplicateCount;
        this.failedCount = failedCount;
        this.completedAt = completedAt;
        this.statusMessage = statusMessage;
        this.failureReason = failureReason;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
