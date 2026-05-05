package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.dto.InfoItemResponse;
import com.aifomo.dashboard.dto.UpdateDecisionStatusRequest;
import com.aifomo.dashboard.service.InfoItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/info-items")
@RequiredArgsConstructor
public class InfoItemController {

    private final InfoItemService infoItemService;

    @GetMapping
    public List<InfoItemResponse> findAll(@RequestParam(defaultValue = "false") boolean includeHidden) {
        return infoItemService.findAll(includeHidden);
    }

    @GetMapping("/{id}")
    public InfoItemResponse findById(@PathVariable Long id) {
        return infoItemService.findById(id);
    }

    @PatchMapping("/{id}/decision-status")
    public InfoItemResponse updateDecisionStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDecisionStatusRequest request
    ) {
        return infoItemService.updateDecisionStatus(id, request.decisionStatus());
    }

    @PatchMapping("/{id}/archive")
    public InfoItemResponse archive(@PathVariable Long id) {
        return infoItemService.archive(id);
    }

    @PatchMapping("/{id}/restore")
    public InfoItemResponse restore(@PathVariable Long id) {
        return infoItemService.restore(id);
    }
}
