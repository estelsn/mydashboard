package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.browser.ThreadsBrowserPageClient;
import com.aifomo.dashboard.collector.threads.browser.ThreadsBrowserPageRequest;
import com.aifomo.dashboard.collector.threads.browser.ThreadsBrowserPageSnapshot;
import com.aifomo.dashboard.collector.threads.browser.ThreadsBrowserPageStatus;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionDescriptor;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionProvider;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionStatus;
import com.aifomo.dashboard.collector.threads.session.ThreadsBrowserSessionProperties;
import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserCommand;
import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserCommandBuilder;
import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserLaunchException;
import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserProcessLauncher;
import com.aifomo.dashboard.domain.source.SourceType;
import com.aifomo.dashboard.dto.ThreadsLoginBrowserResponse;
import com.aifomo.dashboard.dto.ThreadsBrowserSessionResponse;
import com.aifomo.dashboard.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThreadsBrowserSessionService {

    private final BrowserSessionProvider browserSessionProvider;
    private final ThreadsBrowserSessionProperties sessionProperties;
    private final SourceRepository sourceRepository;
    private final ThreadsBrowserPageClient threadsBrowserPageClient;
    private final ThreadsLoginBrowserCommandBuilder loginBrowserCommandBuilder;
    private final ThreadsLoginBrowserProcessLauncher loginBrowserProcessLauncher;

    public ThreadsBrowserSessionResponse getSessionStatus() {
        BrowserSessionDescriptor session = browserSessionProvider.getSession();
        if (session.status() != BrowserSessionStatus.READY || session.profileDirectory() == null) {
            log.info("Threads session check: userDataDir={}, targetUrl={}, loggedIn=false, status={}",
                    session.profileDirectory() == null ? null : session.profileDirectory().toAbsolutePath(),
                    validationUrl(),
                    session.status());
            return ThreadsBrowserSessionResponse.from(session);
        }

        String targetUrl = validationUrl();
        log.info("Threads session check start: userDataDir={}, targetUrl={}",
                session.profileDirectory().toAbsolutePath(),
                targetUrl);
        ThreadsBrowserPageSnapshot snapshot = threadsBrowserPageClient.fetch(new ThreadsBrowserPageRequest(
                targetUrl,
                session.profileDirectory(),
                true,
                0,
                Duration.ofSeconds(15)
        ));
        BrowserSessionDescriptor merged = mergeSessionStatus(session, snapshot);
        log.info("Threads session check result: userDataDir={}, targetUrl={}, loggedIn={}, status={}",
                session.profileDirectory().toAbsolutePath(),
                targetUrl,
                merged.status() == BrowserSessionStatus.READY,
                merged.status());
        return ThreadsBrowserSessionResponse.from(merged);
    }

    public ThreadsLoginBrowserResponse openLoginBrowser() {
        ThreadsLoginBrowserCommand loginBrowserCommand = loginBrowserCommandBuilder.build();
        Path profileDirectory = loginBrowserCommand.profileDirectory();

        try {
            if (Files.exists(profileDirectory) && !Files.isDirectory(profileDirectory)) {
                throw new ThreadsLoginBrowserLaunchException("Threads browser profile path is not a directory");
            }
            Files.createDirectories(profileDirectory);
            log.info("Threads login browser launch: userDataDir={}, loginUrl={}, args={}",
                    profileDirectory.toAbsolutePath(),
                    loginBrowserCommand.loginUrl(),
                    loginBrowserCommand.command());
            loginBrowserProcessLauncher.launch(loginBrowserCommand.command());
        } catch (ThreadsLoginBrowserLaunchException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ThreadsLoginBrowserLaunchException("Failed to open Chrome for Threads login", exception);
        }

        return new ThreadsLoginBrowserResponse(
                "OPENED",
                profileDirectory.toString(),
                loginBrowserCommand.loginUrl(),
                "Chrome을 Threads 전용 user-data-dir로 열었습니다. 로그인 후 브라우저를 닫고 세션 상태를 다시 확인하세요."
        );
    }

    private BrowserSessionDescriptor mergeSessionStatus(
            BrowserSessionDescriptor currentSession,
            ThreadsBrowserPageSnapshot snapshot
    ) {
        return switch (snapshot.status()) {
            case SUCCESS -> currentSession;
            case EMPTY_RESULT -> new BrowserSessionDescriptor(
                    BrowserSessionStatus.ERROR,
                    currentSession.profileDirectory(),
                    "Threads 세션 확인 페이지가 비어 있습니다."
            );
            case LOGIN_REQUIRED -> new BrowserSessionDescriptor(
                    BrowserSessionStatus.LOGIN_REQUIRED,
                    currentSession.profileDirectory(),
                    "Threads 로그인이 필요합니다."
            );
            case ACCESS_RESTRICTED -> new BrowserSessionDescriptor(
                    BrowserSessionStatus.ERROR,
                    currentSession.profileDirectory(),
                    "Threads 접근이 제한되었습니다."
            );
            case TIMEOUT -> new BrowserSessionDescriptor(
                    BrowserSessionStatus.ERROR,
                    currentSession.profileDirectory(),
                    "Threads 세션 확인 중 시간 초과가 발생했습니다."
            );
            case FAILED -> new BrowserSessionDescriptor(
                    BrowserSessionStatus.ERROR,
                    currentSession.profileDirectory(),
                    snapshot.message() == null || snapshot.message().isBlank()
                            ? "Threads 세션 확인에 실패했습니다."
                            : snapshot.message()
            );
        };
    }

    private String validationUrl() {
        return sourceRepository.findByEnabledTrueAndSourceTypeInOrderByPriorityAscIdAsc(
                        java.util.List.of(SourceType.THREADS_ACCOUNT))
                .stream()
                .findFirst()
                .map(source -> source.getUrl())
                .orElse(sessionProperties.getLoginUrl());
    }
}
