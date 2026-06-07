package com.aifomo.dashboard.collector.threads.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.threads.browser-session")
public class ThreadsBrowserSessionProperties {

    public static final Path DEFAULT_PROFILE_DIRECTORY = Path.of("./runtime/browser-profiles/threads");
    public static final String DEFAULT_CHROME_EXECUTABLE =
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
    public static final String DEFAULT_LOGIN_URL = "https://www.threads.net/";
    public static final String DEFAULT_PROFILE_NAME = "Default";

    private Path profileDirectory = DEFAULT_PROFILE_DIRECTORY;
    private String chromeExecutable = DEFAULT_CHROME_EXECUTABLE;
    private String loginUrl = DEFAULT_LOGIN_URL;
    private String profileName = DEFAULT_PROFILE_NAME;

    public Path getProfileDirectory() {
        return profileDirectory;
    }

    public void setProfileDirectory(Path profileDirectory) {
        this.profileDirectory = profileDirectory;
    }

    public String getChromeExecutable() {
        return chromeExecutable;
    }

    public void setChromeExecutable(String chromeExecutable) {
        this.chromeExecutable = chromeExecutable;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }
}
