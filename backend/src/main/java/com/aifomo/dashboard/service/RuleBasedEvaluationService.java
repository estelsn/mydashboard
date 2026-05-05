package com.aifomo.dashboard.service;

import com.aifomo.dashboard.dto.InfoItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleBasedEvaluationService {

    private final EvaluationService evaluationService;

    @Transactional
    public InfoItemResponse evaluateInfoItem(Long id) {
        return evaluationService.recalculateInfoItem(id);
    }

    @Transactional
    public List<InfoItemResponse> evaluateUnreviewedItems() {
        return evaluationService.recalculateUnreviewedItems();
    }
}
