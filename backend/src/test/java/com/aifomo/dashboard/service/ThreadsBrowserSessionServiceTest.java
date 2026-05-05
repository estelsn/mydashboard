package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.session.BrowserSessionDescriptor;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionProvider;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionStatus;
import com.aifomo.dashboard.collector.threads.session.ThreadsBrowserSessionProperties;
import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserCommandBuilder;
import com.aifomo.dashboard.dto.ThreadsLoginBrowserResponse;
import com.aifomo.dashboard.dto.ThreadsBrowserSessionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadsBrowserSessionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsBrowserSessionResponseFromProvider() {
        Path profileDirectory = Path.of("./runtime/browser-profiles/threads");
        BrowserSessionProvider provider = () -> new BrowserSessionDescriptor(
                BrowserSessionStatus.LOGIN_REQUIRED,
                profileDirectory,
                "Open Chrome with the app profile directory and sign in to Threads"
        );

        ThreadsBrowserSessionProperties properties = new ThreadsBrowserSessionProperties();
        ThreadsBrowserSessionService service = new ThreadsBrowserSessionService(
                provider,
                new ThreadsLoginBrowserCommandBuilder(properties),
                command -> {
                }
        );

        ThreadsBrowserSessionResponse response = service.getSessionStatus();

        assertThat(response.status()).isEqualTo(BrowserSessionStatus.LOGIN_REQUIRED);
        assertThat(response.profilePath()).isEqualTo("./runtime/browser-profiles/threads");
        assertThat(response.message()).contains("profile directory");
    }

    @Test
    void opensLoginBrowserWithAppProfileDirectory() {
        Path profileDirectory = tempDir.resolve("runtime/browser-profiles/threads");
        ThreadsBrowserSessionProperties properties = new ThreadsBrowserSessionProperties();
        properties.setProfileDirectory(profileDirectory);
        List<String> launchedCommand = new ArrayList<>();

        ThreadsBrowserSessionService service = new ThreadsBrowserSessionService(
                () -> new BrowserSessionDescriptor(BrowserSessionStatus.LOGIN_REQUIRED, profileDirectory, "login required"),
                new ThreadsLoginBrowserCommandBuilder(properties),
                launchedCommand::addAll
        );

        ThreadsLoginBrowserResponse response = service.openLoginBrowser();

        assertThat(response.status()).isEqualTo("OPENED");
        assertThat(response.profilePath()).isEqualTo(profileDirectory.toString());
        assertThat(response.loginUrl()).isEqualTo("https://www.threads.net/");
        assertThat(launchedCommand).containsExactly(
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "--user-data-dir=" + profileDirectory,
                "--new-window",
                "https://www.threads.net/"
        );
    }
}
