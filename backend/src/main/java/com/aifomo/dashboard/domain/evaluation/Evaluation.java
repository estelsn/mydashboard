package com.aifomo.dashboard.domain.evaluation;

import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.InfoItem;
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
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "info_item_id", nullable = false)
    private InfoItem infoItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionStatus decisionStatus;

    @Lob
    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false)
    private double relevanceScore;

    @Column(nullable = false)
    private double actionabilityScore;

    @Column(nullable = false)
    private double noveltyScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluatorType evaluatorType;

    @Column(nullable = false)
    private String evaluatorVersion;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Evaluation(
            InfoItem infoItem,
            DecisionStatus decisionStatus,
            String reason,
            double confidence,
            double relevanceScore,
            double actionabilityScore,
            double noveltyScore,
            EvaluatorType evaluatorType,
            String evaluatorVersion
    ) {
        this.infoItem = infoItem;
        this.decisionStatus = decisionStatus;
        this.reason = reason;
        this.confidence = confidence;
        this.relevanceScore = relevanceScore;
        this.actionabilityScore = actionabilityScore;
        this.noveltyScore = noveltyScore;
        this.evaluatorType = evaluatorType;
        this.evaluatorVersion = evaluatorVersion;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
