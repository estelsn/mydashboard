package com.aifomo.dashboard.service;

import com.aifomo.dashboard.domain.evaluation.Evaluation;
import com.aifomo.dashboard.domain.evaluation.EvaluatorType;
import com.aifomo.dashboard.domain.info.DecisionStatus;
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
public class RuleBasedEvaluationService {

    private static final Set<DecisionStatus> HIDDEN_STATUSES = EnumSet.of(
            DecisionStatus.IGNORE,
            DecisionStatus.ARCHIVE_CANDIDATE
    );

    private final RuleBasedEvaluator ruleBasedEvaluator;
    private final InfoItemRepository infoItemRepository;
    private final EvaluationRepository evaluationRepository;

    @Transactional
    public InfoItemResponse evaluateInfoItem(Long id) {
        InfoItem infoItem = infoItemRepository.findById(id)
                .orElseThrow(() -> new InfoItemNotFoundException(id));
        Evaluation evaluation = evaluateAndPersist(infoItem);
        return InfoItemResponse.from(infoItem, EvaluationResponse.from(evaluation));
    }

    @Transactional
    public List<InfoItemResponse> evaluateUnreviewedItems() {
        return infoItemRepository
                .findByIsDeletedFalseAndManualOverrideFalseAndDecisionStatusOrderByCollectedAtDesc(DecisionStatus.UNREVIEWED)
                .stream()
                .map(infoItem -> {
                    Evaluation evaluation = evaluateAndPersist(infoItem);
                    return InfoItemResponse.from(infoItem, EvaluationResponse.from(evaluation));
                })
                .toList();
    }

    private Evaluation evaluateAndPersist(InfoItem infoItem) {
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
                EvaluatorType.RULE_BASED,
                RuleBasedEvaluator.EVALUATOR_VERSION
        ));
    }
}
