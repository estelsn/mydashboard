package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.dto.CollectionRunResponse;
import com.aifomo.dashboard.service.CollectionRunQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collection-runs")
@RequiredArgsConstructor
public class CollectionRunController {

    private final CollectionRunQueryService collectionRunQueryService;

    @GetMapping
    public List<CollectionRunResponse> findRecentRuns() {
        return collectionRunQueryService.findRecentRuns();
    }
}
