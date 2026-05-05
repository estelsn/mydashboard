package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.dto.InfoItemResponse;
import com.aifomo.dashboard.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping("/info-items/{id}/recalculate")
    public InfoItemResponse recalculateInfoItem(@PathVariable Long id) {
        return evaluationService.recalculateInfoItem(id);
    }

    @PostMapping("/unreviewed/recalculate")
    public List<InfoItemResponse> recalculateUnreviewedItems() {
        return evaluationService.recalculateUnreviewedItems();
    }
}
