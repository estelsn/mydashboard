package com.aifomo.dashboard.domain.info;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
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
import jakarta.persistence.OneToOne;
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
public class InfoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collected_item_id", nullable = false, unique = true)
    private CollectedItem collectedItem;

    @Column(nullable = false, length = 300)
    private String title;

    @Lob
    @Column(nullable = false)
    private String summary;

    @Column(nullable = false, length = 512)
    private String originalUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceCategory category;

    @Column(nullable = false, length = 1000)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportanceLevel importanceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionStatus decisionStatus;

    @Column(nullable = false)
    private boolean manualOverride;

    @Column(nullable = false)
    private boolean isHidden;

    @Column(nullable = false)
    private boolean isDeleted;

    private Long duplicateOfId;

    @Column(nullable = false)
    private boolean isDuplicate;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime collectedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public InfoItem(
            Source source,
            CollectedItem collectedItem,
            String title,
            String summary,
            String originalUrl,
            SourceCategory category,
            String tags,
            ImportanceLevel importanceLevel,
            DecisionStatus decisionStatus,
            boolean manualOverride,
            boolean isHidden,
            boolean isDeleted,
            Long duplicateOfId,
            boolean isDuplicate,
            LocalDateTime publishedAt,
            LocalDateTime collectedAt
    ) {
        this.source = source;
        this.collectedItem = collectedItem;
        this.title = title;
        this.summary = summary;
        this.originalUrl = originalUrl;
        this.category = category;
        this.tags = tags;
        this.importanceLevel = importanceLevel;
        this.decisionStatus = decisionStatus;
        this.manualOverride = manualOverride;
        this.isHidden = isHidden;
        this.isDeleted = isDeleted;
        this.duplicateOfId = duplicateOfId;
        this.isDuplicate = isDuplicate;
        this.publishedAt = publishedAt;
        this.collectedAt = collectedAt;
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
