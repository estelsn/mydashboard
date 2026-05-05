package com.aifomo.dashboard.collector.threads;

public interface ThreadsCollector {

    ThreadsCollectionResult collect(ThreadsCollectionRequest request);
}
