package com.aifomo.dashboard.collector.threads.session;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ThreadsLoginBrowserCommandBuilder {

    private final ThreadsBrowserSessionProperties properties;

    public ThreadsLoginBrowserCommand build() {
        Path profileDirectory = properties.getProfileDirectory();
        String chromeExecutable = properties.getChromeExecutable();
        String loginUrl = properties.getLoginUrl();

        if (profileDirectory == null) {
            throw new ThreadsLoginBrowserLaunchException("Threads browser profile directory is not configured");
        }
        if (chromeExecutable == null || chromeExecutable.isBlank()) {
            throw new ThreadsLoginBrowserLaunchException("Chrome executable path is not configured");
        }
        if (loginUrl == null || loginUrl.isBlank()) {
            throw new ThreadsLoginBrowserLaunchException("Threads login URL is not configured");
        }

        Path normalizedProfileDirectory = profileDirectory.normalize();
        return new ThreadsLoginBrowserCommand(
                List.of(
                        chromeExecutable,
                        "--user-data-dir=" + normalizedProfileDirectory,
                        "--new-window",
                        loginUrl
                ),
                normalizedProfileDirectory,
                loginUrl
        );
    }
}
