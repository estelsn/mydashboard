package com.aifomo.dashboard.collector.threads.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.threads.browser-session")
public class ThreadsBrowserSessionProperties {

    public static final Path DEFAULT_PROFILE_DIRECTORY = Path.of("./runtime/browser-profiles/threads");

    private Path profileDirectory = DEFAULT_PROFILE_DIRECTORY;

    public Path getProfileDirectory() {
        return profileDirectory;
    }

    public void setProfileDirectory(Path profileDirectory) {
        this.profileDirectory = profileDirectory;
    }
}
