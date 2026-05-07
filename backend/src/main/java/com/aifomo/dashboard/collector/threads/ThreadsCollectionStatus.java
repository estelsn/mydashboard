package com.aifomo.dashboard.collector.threads;

public enum ThreadsCollectionStatus {
    SUCCESS,
    LOGIN_REQUIRED,
    ACCESS_RESTRICTED,
    EMPTY_RESULT,
    COOLDOWN_SKIPPED,
    TIMEOUT,
    FAILED
}
