package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.dto.SourceResponse;
import com.aifomo.dashboard.service.SourceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
