package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.dto.ThreadsBrowserSessionResponse;
import com.aifomo.dashboard.dto.ThreadsLoginBrowserResponse;
import com.aifomo.dashboard.service.ThreadsBrowserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/threads/session")
@RequiredArgsConstructor
public class ThreadsSessionController {

    private final ThreadsBrowserSessionService threadsBrowserSessionService;

    @GetMapping
    public ThreadsBrowserSessionResponse getSessionStatus() {
        return threadsBrowserSessionService.getSessionStatus();
    }

    @PostMapping("/open-login")
    public ThreadsLoginBrowserResponse openLoginBrowser() {
        return threadsBrowserSessionService.openLoginBrowser();
    }
}
