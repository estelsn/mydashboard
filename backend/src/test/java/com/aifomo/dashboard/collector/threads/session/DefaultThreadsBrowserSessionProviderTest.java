package com.aifomo.dashboard.collector.threads.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultThreadsBrowserSessionProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void exposesDefaultProfileDirectory() {
        assertThat(ThreadsBrowserSessionProperties.DEFAULT_PROFILE_DIRECTORY)
                .isEqualTo(Path.of("./runtime/browser-profiles/threads"));
    }

    @Test
    void returnsNotConfiguredWhenProfileDirectoryIsMissingFromConfig() {
        ThreadsBrowserSessionProperties properties = new ThreadsBrowserSessionProperties();
        properties.setProfileDirectory(null);

        BrowserSessionDescriptor descriptor = new DefaultThreadsBrowserSessionProvider(properties).getSession();

        assertThat(descriptor.status()).isEqualTo(BrowserSessionStatus.NOT_CONFIGURED);
        assertThat(descriptor.profileDirectory()).isNull();
    }

    @Test
    void returnsLoginRequiredWhenProfileDirectoryDoesNotExist() {
        Path profileDirectory = tempDir.resolve("runtime/browser-profiles/threads");
        ThreadsBrowserSessionProperties properties = new ThreadsBrowserSessionProperties();
        properties.setProfileDirectory(profileDirectory);

        BrowserSessionDescriptor descriptor = new DefaultThreadsBrowserSessionProvider(properties).getSession();

        assertThat(descriptor.status()).isEqualTo(BrowserSessionStatus.LOGIN_REQUIRED);
        assertThat(descriptor.profileDirectory()).isEqualTo(profileDirectory);
    }

    @Test
    void returnsReadyWhenProfileDirectoryExists() throws Exception {
        Path profileDirectory = Files.createDirectories(tempDir.resolve("runtime/browser-profiles/threads"));
        ThreadsBrowserSessionProperties properties = new ThreadsBrowserSessionProperties();
        properties.setProfileDirectory(profileDirectory);

        BrowserSessionDescriptor descriptor = new DefaultThreadsBrowserSessionProvider(properties).getSession();

        assertThat(descriptor.status()).isEqualTo(BrowserSessionStatus.READY);
        assertThat(descriptor.profileDirectory()).isEqualTo(profileDirectory);
    }

    @Test
    void returnsErrorWhenProfilePathIsNotDirectory() throws Exception {
        Path profilePath = Files.createFile(tempDir.resolve("threads-profile"));
        ThreadsBrowserSessionProperties properties = new ThreadsBrowserSessionProperties();
        properties.setProfileDirectory(profilePath);

        BrowserSessionDescriptor descriptor = new DefaultThreadsBrowserSessionProvider(properties).getSession();

        assertThat(descriptor.status()).isEqualTo(BrowserSessionStatus.ERROR);
        assertThat(descriptor.profileDirectory()).isEqualTo(profilePath);
    }
}
