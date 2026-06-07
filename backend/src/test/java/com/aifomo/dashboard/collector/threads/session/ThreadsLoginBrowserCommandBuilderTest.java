package com.aifomo.dashboard.collector.threads.session;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThreadsLoginBrowserCommandBuilderTest {

    @Test
    void buildsMacChromeCommandWithDefaultProfileDirectoryAndThreadsUrl() {
        ThreadsLoginBrowserCommand command = new ThreadsLoginBrowserCommandBuilder(
                new ThreadsBrowserSessionProperties()
        ).build();

        assertThat(command.profileDirectory()).isEqualTo(Path.of("runtime/browser-profiles/threads"));
        assertThat(command.loginUrl()).isEqualTo("https://www.threads.net/");
        assertThat(command.command()).containsExactly(
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "--user-data-dir=runtime/browser-profiles/threads",
                "--profile-directory=Default",
                "--restore-last-session",
                "--new-window",
                "https://www.threads.net/"
        );
    }

    @Test
    void usesConfiguredChromeExecutableProfileDirectoryAndLoginUrl() {
        ThreadsBrowserSessionProperties properties = new ThreadsBrowserSessionProperties();
        properties.setChromeExecutable("/custom/chrome");
        properties.setProfileDirectory(Path.of("./build/test-runtime/threads profile"));
        properties.setLoginUrl("https://www.threads.net/@example");

        ThreadsLoginBrowserCommand command = new ThreadsLoginBrowserCommandBuilder(properties).build();

        assertThat(command.command()).containsExactly(
                "/custom/chrome",
                "--user-data-dir=build/test-runtime/threads profile",
                "--profile-directory=Default",
                "--restore-last-session",
                "--new-window",
                "https://www.threads.net/@example"
        );
    }

    @Test
    void rejectsMissingChromeExecutable() {
        ThreadsBrowserSessionProperties properties = new ThreadsBrowserSessionProperties();
        properties.setChromeExecutable(" ");

        assertThatThrownBy(() -> new ThreadsLoginBrowserCommandBuilder(properties).build())
                .isInstanceOf(ThreadsLoginBrowserLaunchException.class)
                .hasMessage("Chrome executable path is not configured");
    }
}
