package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.dto.DashboardSummaryResponse;
import com.aifomo.dashboard.service.InfoItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final InfoItemService infoItemService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return infoItemService.getDashboardSummary();
    }
}
