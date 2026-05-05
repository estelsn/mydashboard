package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.session.BrowserSessionDescriptor;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionProvider;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionStatus;
import com.aifomo.dashboard.dto.ThreadsBrowserSessionResponse;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadsBrowserSessionServiceTest {

    @Test
    void returnsBrowserSessionResponseFromProvider() {
        Path profileDirectory = Path.of("./runtime/browser-profiles/threads");
        BrowserSessionProvider provider = () -> new BrowserSessionDescriptor(
                BrowserSessionStatus.LOGIN_REQUIRED,
                profileDirectory,
                "Open Chrome with the app profile directory and sign in to Threads"
        );

        ThreadsBrowserSessionResponse response = new ThreadsBrowserSessionService(provider).getSessionStatus();

        assertThat(response.status()).isEqualTo(BrowserSessionStatus.LOGIN_REQUIRED);
        assertThat(response.profileDirectory()).isEqualTo("./runtime/browser-profiles/threads");
        assertThat(response.message()).contains("profile directory");
    }
}
