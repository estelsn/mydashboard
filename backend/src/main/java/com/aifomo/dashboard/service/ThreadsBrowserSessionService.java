package com.aifomo.dashboard.service;

import com.aifomo.dashboard.collector.threads.session.BrowserSessionProvider;
import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserCommand;
import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserCommandBuilder;
import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserLaunchException;
import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserProcessLauncher;
import com.aifomo.dashboard.dto.ThreadsLoginBrowserResponse;
import com.aifomo.dashboard.dto.ThreadsBrowserSessionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class ThreadsBrowserSessionService {

    private final BrowserSessionProvider browserSessionProvider;
    private final ThreadsLoginBrowserCommandBuilder loginBrowserCommandBuilder;
    private final ThreadsLoginBrowserProcessLauncher loginBrowserProcessLauncher;

    public ThreadsBrowserSessionResponse getSessionStatus() {
        return ThreadsBrowserSessionResponse.from(browserSessionProvider.getSession());
    }

    public ThreadsLoginBrowserResponse openLoginBrowser() {
        ThreadsLoginBrowserCommand loginBrowserCommand = loginBrowserCommandBuilder.build();
        Path profileDirectory = loginBrowserCommand.profileDirectory();

        try {
            if (Files.exists(profileDirectory) && !Files.isDirectory(profileDirectory)) {
                throw new ThreadsLoginBrowserLaunchException("Threads browser profile path is not a directory");
            }
            Files.createDirectories(profileDirectory);
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
                "Chrome opened with the app Threads profile directory"
        );
    }
}
