package com.aifomo.dashboard.service;

import com.aifomo.dashboard.domain.evaluation.Evaluation;
import com.aifomo.dashboard.domain.evaluation.EvaluatorType;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.ImportanceLevel;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.dto.EvaluationResponse;
import com.aifomo.dashboard.dto.InfoItemResponse;
import com.aifomo.dashboard.repository.EvaluationRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    public static final String MANUAL_EVALUATOR_VERSION = "manual-v1";
    public static final String LLM_READY_STUB_EVALUATOR_VERSION = "llm-ready-stub-v1";

    private static final Set<DecisionStatus> HIDDEN_STATUSES = EnumSet.of(
            DecisionStatus.IGNORE,
            DecisionStatus.ARCHIVE_CANDIDATE
    );

    private final RuleBasedEvaluator ruleBasedEvaluator;
    private final InfoItemRepository infoItemRepository;
    private final EvaluationRepository evaluationRepository;

    @Transactional
    public InfoItemResponse recalculateInfoItem(Long id) {
        InfoItem infoItem = infoItemRepository.findById(id)
                .orElseThrow(() -> new InfoItemNotFoundException(id));
        Evaluation evaluation = evaluateRuleBasedAndPersist(infoItem);
        return InfoItemResponse.from(infoItem, EvaluationResponse.from(evaluation));
    }

    @Transactional
    public List<InfoItemResponse> recalculateUnreviewedItems() {
        return infoItemRepository
                .findByIsDeletedFalseAndManualOverrideFalseAndDecisionStatusOrderByCollectedAtDesc(DecisionStatus.UNREVIEWED)
                .stream()
                .map(infoItem -> {
                    Evaluation evaluation = evaluateRuleBasedAndPersist(infoItem);
                    return InfoItemResponse.from(infoItem, EvaluationResponse.from(evaluation));
                })
                .toList();
    }

    @Transactional
    public Evaluation createManualEvaluation(InfoItem infoItem, DecisionStatus decisionStatus) {
        return evaluationRepository.save(new Evaluation(
                infoItem,
                decisionStatus,
                "사용자가 분류 상태를 직접 변경했습니다.",
                1.0,
                manualRelevanceScore(decisionStatus),
                manualActionabilityScore(decisionStatus),
                0.5,
                EvaluatorType.MANUAL,
                MANUAL_EVALUATOR_VERSION
        ));
    }

    @Transactional
    public Evaluation createLlmReadyStubEvaluation(InfoItem infoItem) {
        return evaluationRepository.save(new Evaluation(
                infoItem,
                infoItem.getDecisionStatus(),
                "외부 API 호출 없이 LLM 평가 준비 상태만 기록했습니다.",
                0.0,
                0.0,
                0.0,
                0.0,
                EvaluatorType.LLM_READY_STUB,
                LLM_READY_STUB_EVALUATOR_VERSION
        ));
    }

    private Evaluation evaluateRuleBasedAndPersist(InfoItem infoItem) {
        RuleBasedEvaluationResult result = ruleBasedEvaluator.evaluate(infoItem);

        if (!infoItem.isManualOverride()) {
            infoItem.setDecisionStatus(result.decisionStatus());
            infoItem.setImportanceLevel(result.importanceLevel());
            infoItem.setHidden(HIDDEN_STATUSES.contains(result.decisionStatus()));
        }

        return evaluationRepository.save(new Evaluation(
                infoItem,
                result.decisionStatus(),
                result.reason(),
                result.confidence(),
                result.relevanceScore(),
                result.actionabilityScore(),
                result.noveltyScore(),
                EvaluatorType.RULE_BASED_STUB,
                RuleBasedEvaluator.EVALUATOR_VERSION
        ));
    }

    private double manualRelevanceScore(DecisionStatus decisionStatus) {
        return switch (decisionStatus) {
            case APPLY -> 1.0;
            case HOLD -> 0.75;
            case UNREVIEWED -> 0.5;
            case IGNORE, ARCHIVE_CANDIDATE -> 0.2;
        };
    }

    private double manualActionabilityScore(DecisionStatus decisionStatus) {
        return switch (decisionStatus) {
            case APPLY -> 1.0;
            case HOLD -> 0.45;
            case UNREVIEWED -> 0.25;
            case IGNORE, ARCHIVE_CANDIDATE -> 0.05;
        };
    }
}
