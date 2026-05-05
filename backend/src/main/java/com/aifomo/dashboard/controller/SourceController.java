package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.dto.SourceResponse;
import com.aifomo.dashboard.dto.UpdateSourceEnabledRequest;
import jakarta.validation.Valid;
import com.aifomo.dashboard.service.SourceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
public class SourceController {

    private final SourceQueryService sourceQueryService;

    @GetMapping
    public List<SourceResponse> findAll() {
        return sourceQueryService.findAll();
    }

    @PatchMapping("/{id}/enabled")
    public SourceResponse updateEnabled(@PathVariable Long id, @Valid @RequestBody UpdateSourceEnabledRequest request) {
        return sourceQueryService.updateEnabled(id, request.enabled());
    }
}
