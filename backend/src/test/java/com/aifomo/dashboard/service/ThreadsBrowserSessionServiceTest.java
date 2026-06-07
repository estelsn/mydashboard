package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.browser.ThreadsBrowserPageClient;
import com.aifomo.dashboard.collector.threads.browser.ThreadsBrowserPageSnapshot;
import com.aifomo.dashboard.collector.threads.browser.ThreadsBrowserPageStatus;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionDescriptor;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionProvider;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionStatus;
import com.aifomo.dashboard.collector.threads.session.ThreadsBrowserSessionProperties;
import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserCommandBuilder;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import com.aifomo.dashboard.dto.ThreadsLoginBrowserResponse;
import com.aifomo.dashboard.dto.ThreadsBrowserSessionResponse;
import com.aifomo.dashboard.repository.SourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
                properties,
                sourceRepository(),
                request -> ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.LOGIN_REQUIRED, "Threads 로그인이 필요합니다."),
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
                properties,
                sourceRepository(),
                request -> ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.LOGIN_REQUIRED, "Threads 로그인이 필요합니다."),
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
                "--profile-directory=Default",
                "--restore-last-session",
                "--new-window",
                "https://www.threads.net/"
        );
    }

    @Test
    void downgradesReadySessionToLoginRequiredWhenValidationPageRequiresLogin() {
        Path profileDirectory = tempDir.resolve("runtime/browser-profiles/threads");
        BrowserSessionProvider provider = () -> new BrowserSessionDescriptor(
                BrowserSessionStatus.READY,
                profileDirectory,
                "Threads browser profile directory is available"
        );
        ThreadsBrowserSessionProperties properties = new ThreadsBrowserSessionProperties();
        ThreadsBrowserPageClient pageClient = request -> ThreadsBrowserPageSnapshot.failure(
                ThreadsBrowserPageStatus.LOGIN_REQUIRED,
                "Threads 로그인이 필요합니다."
        );

        ThreadsBrowserSessionService service = new ThreadsBrowserSessionService(
                provider,
                properties,
                sourceRepository(),
                pageClient,
                new ThreadsLoginBrowserCommandBuilder(properties),
                command -> {
                }
        );

        ThreadsBrowserSessionResponse response = service.getSessionStatus();

        assertThat(response.status()).isEqualTo(BrowserSessionStatus.LOGIN_REQUIRED);
        assertThat(response.message()).isEqualTo("Threads 로그인이 필요합니다.");
    }

    private SourceRepository sourceRepository() {
        SourceRepository repository = Mockito.mock(SourceRepository.class);
        when(repository.findByEnabledTrueAndSourceTypeInOrderByPriorityAscIdAsc(List.of(SourceType.THREADS_ACCOUNT)))
                .thenReturn(List.of(new Source(
                        "Threads Source",
                        SourceType.THREADS_ACCOUNT,
                        SourceCategory.NEWS,
                        "https://www.threads.com/@example",
                        "test",
                        true,
                        10
                )));
        return repository;
    }
}
