package com.aifomo.dashboard.service;

import com.aifomo.dashboard.domain.evaluation.Evaluation;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.dto.DashboardSummaryResponse;
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
public class InfoItemService {

    private static final Set<DecisionStatus> DEFAULT_HIDDEN_STATUSES = EnumSet.of(
            DecisionStatus.IGNORE,
            DecisionStatus.ARCHIVE_CANDIDATE
    );

    private final InfoItemRepository infoItemRepository;
    private final EvaluationRepository evaluationRepository;

    @Transactional(readOnly = true)
    public List<InfoItemResponse> findAll(boolean includeHidden) {
        List<InfoItem> infoItems = includeHidden
                ? infoItemRepository.findByIsDeletedFalseOrderByCollectedAtDesc()
                : infoItemRepository.findByIsDeletedFalseAndIsHiddenFalseAndDecisionStatusNotInOrderByCollectedAtDesc(DEFAULT_HIDDEN_STATUSES);

        return infoItems.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InfoItemResponse findById(Long id) {
        return toResponse(getInfoItem(id));
    }

    @Transactional
    public InfoItemResponse updateDecisionStatus(Long id, DecisionStatus decisionStatus) {
        InfoItem infoItem = getInfoItem(id);
        infoItem.setDecisionStatus(decisionStatus);
        infoItem.setManualOverride(true);
        infoItem.setHidden(DEFAULT_HIDDEN_STATUSES.contains(decisionStatus));
        return toResponse(infoItem);
    }

    @Transactional
    public InfoItemResponse archive(Long id) {
        InfoItem infoItem = getInfoItem(id);
        infoItem.setDecisionStatus(DecisionStatus.ARCHIVE_CANDIDATE);
        infoItem.setHidden(true);
        infoItem.setManualOverride(true);
        return toResponse(infoItem);
    }

    @Transactional
    public InfoItemResponse restore(Long id) {
        InfoItem infoItem = getInfoItem(id);
        infoItem.setHidden(false);
        if (infoItem.getDecisionStatus() == DecisionStatus.ARCHIVE_CANDIDATE) {
            infoItem.setDecisionStatus(DecisionStatus.UNREVIEWED);
        }
        infoItem.setManualOverride(true);
        return toResponse(infoItem);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        long totalCount = infoItemRepository.count();
        long hiddenCount = infoItemRepository.countByIsDeletedFalseAndIsHiddenTrue();
        long ignoreCount = infoItemRepository.countByIsDeletedFalseAndDecisionStatus(DecisionStatus.IGNORE);
        long archiveCandidateCount = infoItemRepository.countByIsDeletedFalseAndDecisionStatus(DecisionStatus.ARCHIVE_CANDIDATE);
        long visibleCount = infoItemRepository.findByIsDeletedFalseAndIsHiddenFalseAndDecisionStatusNotInOrderByCollectedAtDesc(DEFAULT_HIDDEN_STATUSES).size();

        return new DashboardSummaryResponse(
                totalCount,
                visibleCount,
                infoItemRepository.countByIsDeletedFalseAndIsHiddenFalseAndDecisionStatus(DecisionStatus.APPLY),
                infoItemRepository.countByIsDeletedFalseAndIsHiddenFalseAndDecisionStatus(DecisionStatus.HOLD),
                infoItemRepository.countByIsDeletedFalseAndIsHiddenFalseAndDecisionStatus(DecisionStatus.UNREVIEWED),
                ignoreCount,
                archiveCandidateCount,
                hiddenCount
        );
    }

    private InfoItem getInfoItem(Long id) {
        return infoItemRepository.findById(id)
                .orElseThrow(() -> new InfoItemNotFoundException(id));
    }

    private InfoItemResponse toResponse(InfoItem infoItem) {
        EvaluationResponse latestEvaluation = evaluationRepository.findFirstByInfoItemOrderByCreatedAtDesc(infoItem)
                .map(EvaluationResponse::from)
                .orElse(null);
        return InfoItemResponse.from(infoItem, latestEvaluation);
    }
}
