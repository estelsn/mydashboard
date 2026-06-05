package com.aifomo.dashboard.collector.threads;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

@ConfigurationProperties(prefix = "app.threads.collection")
public class ThreadsCollectionProperties {

    private final Defaults defaults = new Defaults();
    private final Limits limits = new Limits();
    private final Safety safety = new Safety();

    public Defaults getDefaults() {
        return defaults;
    }

    public Limits getLimits() {
        return limits;
    }

    public Safety getSafety() {
        return safety;
    }

    public static class Defaults {
        private int maxPostsPerAccount = 3;
        private int maxScrollCount = 8;

        public int getMaxPostsPerAccount() {
            return maxPostsPerAccount;
        }

        public void setMaxPostsPerAccount(int maxPostsPerAccount) {
            this.maxPostsPerAccount = maxPostsPerAccount;
        }

        public int getMaxScrollCount() {
            return maxScrollCount;
        }

        public void setMaxScrollCount(int maxScrollCount) {
            this.maxScrollCount = maxScrollCount;
        }
    }

    public static class Limits {
        private int maxPostsPerAccount = 5;
        private int maxScrollCount = 12;

        public int getMaxPostsPerAccount() {
            return maxPostsPerAccount;
        }

        public void setMaxPostsPerAccount(int maxPostsPerAccount) {
            this.maxPostsPerAccount = maxPostsPerAccount;
        }

        public int getMaxScrollCount() {
            return maxScrollCount;
        }

        public void setMaxScrollCount(int maxScrollCount) {
            this.maxScrollCount = maxScrollCount;
        }
    }

    public static class Safety {
        private Duration delayBetweenAccounts = Duration.ofSeconds(5);
        private Duration delayBetweenScrolls = Duration.ofSeconds(3);
        private Duration minSourceRecollectionInterval = Duration.ofHours(1);
        private Set<ThreadsCollectionStatus> stopOnStatuses = EnumSet.of(
                ThreadsCollectionStatus.ACCESS_RESTRICTED,
                ThreadsCollectionStatus.LOGIN_REQUIRED,
                ThreadsCollectionStatus.TIMEOUT
        );

        public Duration getDelayBetweenAccounts() {
            return delayBetweenAccounts;
        }

        public void setDelayBetweenAccounts(Duration delayBetweenAccounts) {
            this.delayBetweenAccounts = delayBetweenAccounts;
        }

        public Duration getDelayBetweenScrolls() {
            return delayBetweenScrolls;
        }

        public void setDelayBetweenScrolls(Duration delayBetweenScrolls) {
            this.delayBetweenScrolls = delayBetweenScrolls;
        }

        public Duration getMinSourceRecollectionInterval() {
            return minSourceRecollectionInterval;
        }

        public void setMinSourceRecollectionInterval(Duration minSourceRecollectionInterval) {
            this.minSourceRecollectionInterval = minSourceRecollectionInterval;
        }

        public Set<ThreadsCollectionStatus> getStopOnStatuses() {
            return stopOnStatuses;
        }

        public void setStopOnStatuses(Set<ThreadsCollectionStatus> stopOnStatuses) {
            this.stopOnStatuses = stopOnStatuses == null || stopOnStatuses.isEmpty()
                    ? EnumSet.noneOf(ThreadsCollectionStatus.class)
                    : EnumSet.copyOf(stopOnStatuses);
        }
    }
}
