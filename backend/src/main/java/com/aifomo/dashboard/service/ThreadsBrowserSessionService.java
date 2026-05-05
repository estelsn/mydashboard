package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.session.BrowserSessionProvider;
import com.aifomo.dashboard.dto.ThreadsBrowserSessionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThreadsBrowserSessionService {

    private final BrowserSessionProvider browserSessionProvider;

    public ThreadsBrowserSessionResponse getSessionStatus() {
        return ThreadsBrowserSessionResponse.from(browserSessionProvider.getSession());
    }
}
