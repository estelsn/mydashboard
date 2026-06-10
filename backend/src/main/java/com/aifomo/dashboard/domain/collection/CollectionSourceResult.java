package com.aifomo.dashboard.domain.collection;

import com.aifomo.dashboard.collector.threads.ThreadsCollectionStatus;
import com.aifomo.dashboard.domain.source.Source;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionSourceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_run_id", nullable = false)
    private CollectionRun collectionRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreadsCollectionStatus status;

    @Column(nullable = false)
    private int collectedCount;

    @Column(nullable = false)
    private int createdCount;

    @Column(nullable = false)
    private int duplicateCount;

    @Column(nullable = false)
    private int failedCount;

    @Lob
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public CollectionSourceResult(
            CollectionRun collectionRun,
            Source source,
            ThreadsCollectionStatus status,
            int collectedCount,
            int createdCount,
            int duplicateCount,
            int failedCount,
            String message
    ) {
        this.collectionRun = collectionRun;
        this.source = source;
        this.status = status;
        this.collectedCount = collectedCount;
        this.createdCount = createdCount;
        this.duplicateCount = duplicateCount;
        this.failedCount = failedCount;
        this.message = message;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
