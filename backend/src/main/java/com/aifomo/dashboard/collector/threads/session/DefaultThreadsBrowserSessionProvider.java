package com.aifomo.dashboard.collector.threads.session;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(ThreadsBrowserSessionProperties.class)
public class DefaultThreadsBrowserSessionProvider implements BrowserSessionProvider {

    private final ThreadsBrowserSessionProperties properties;

    @Override
    public BrowserSessionDescriptor getSession() {
        Path profileDirectory = properties.getProfileDirectory();
        if (profileDirectory == null) {
            return new BrowserSessionDescriptor(
                    BrowserSessionStatus.NOT_CONFIGURED,
                    null,
                    "Threads browser profile directory is not configured"
            );
        }

        Path normalizedProfileDirectory = profileDirectory.normalize();
        if (!Files.exists(normalizedProfileDirectory)) {
            return new BrowserSessionDescriptor(
                    BrowserSessionStatus.LOGIN_REQUIRED,
                    normalizedProfileDirectory,
                    "Open Chrome with the app profile directory and sign in to Threads"
            );
        }
        if (!Files.isDirectory(normalizedProfileDirectory)) {
            return new BrowserSessionDescriptor(
                    BrowserSessionStatus.ERROR,
                    normalizedProfileDirectory,
                    "Threads browser profile path is not a directory"
            );
        }
        if (!Files.isReadable(normalizedProfileDirectory)) {
            return new BrowserSessionDescriptor(
                    BrowserSessionStatus.ERROR,
                    normalizedProfileDirectory,
                    "Threads browser profile directory is not readable"
            );
        }

        return new BrowserSessionDescriptor(
                BrowserSessionStatus.READY,
                normalizedProfileDirectory,
                "Threads browser profile directory is available"
        );
    }
}
