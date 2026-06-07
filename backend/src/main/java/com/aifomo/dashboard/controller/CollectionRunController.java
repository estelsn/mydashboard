package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.dto.CollectionRunResponse;
import com.aifomo.dashboard.dto.ManualThreadsCollectionRequest;
import com.aifomo.dashboard.dto.ManualThreadsCollectionResponse;
import com.aifomo.dashboard.service.CollectionRunQueryService;
import com.aifomo.dashboard.service.ManualThreadsCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collection-runs")
@RequiredArgsConstructor
public class CollectionRunController {

    private final CollectionRunQueryService collectionRunQueryService;
    private final ManualThreadsCollectionService manualThreadsCollectionService;

    @GetMapping
    public List<CollectionRunResponse> findRecentRuns() {
        return collectionRunQueryService.findRecentRuns();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRun(@PathVariable Long id) {
        collectionRunQueryService.deleteRun(id);
    }

    @PostMapping("/threads")
    public ManualThreadsCollectionResponse collectThreads(@Valid @RequestBody ManualThreadsCollectionRequest request) {
        return manualThreadsCollectionService.collect(request);
    }

    @PostMapping("/threads/recent")
    public ManualThreadsCollectionResponse collectRecentThreads() {
        return manualThreadsCollectionService.collectRecentFromEnabledSources();
    }
}
