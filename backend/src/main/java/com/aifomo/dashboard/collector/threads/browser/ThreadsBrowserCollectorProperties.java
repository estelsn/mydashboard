package com.aifomo.dashboard.collector.threads.browser;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.threads.browser-collector")
public class ThreadsBrowserCollectorProperties {

    private boolean headless = false;
    private int maxScrollCount = 5;
    private int maxPostsPerAccount = 20;
    private Duration timeout = Duration.ofSeconds(20);

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public int getMaxScrollCount() {
        return maxScrollCount;
    }

    public void setMaxScrollCount(int maxScrollCount) {
        this.maxScrollCount = maxScrollCount;
    }

    public int getMaxPostsPerAccount() {
        return maxPostsPerAccount;
    }

    public void setMaxPostsPerAccount(int maxPostsPerAccount) {
        this.maxPostsPerAccount = maxPostsPerAccount;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
