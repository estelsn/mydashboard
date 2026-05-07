package com.aifomo.dashboard.collector.threads;

import org.springframework.stereotype.Component;

import java.time.Duration;

public interface ThreadsCollectionSleeper {

    void sleep(Duration duration);
}

@Component
class ThreadSleepThreadsCollectionSleeper implements ThreadsCollectionSleeper {

    @Override
    public void sleep(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Threads collection delay was interrupted", ex);
        }
    }
}
