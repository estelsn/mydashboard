package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.dto.InfoItemResponse;
import com.aifomo.dashboard.service.RuleBasedEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rule-based-evaluations")
@RequiredArgsConstructor
public class RuleBasedEvaluationController {

    private final RuleBasedEvaluationService ruleBasedEvaluationService;

    @PostMapping("/unreviewed")
    public List<InfoItemResponse> evaluateUnreviewedItems() {
        return ruleBasedEvaluationService.evaluateUnreviewedItems();
    }

    @PostMapping("/info-items/{id}")
    public InfoItemResponse evaluateInfoItem(@PathVariable Long id) {
        return ruleBasedEvaluationService.evaluateInfoItem(id);
    }
}
