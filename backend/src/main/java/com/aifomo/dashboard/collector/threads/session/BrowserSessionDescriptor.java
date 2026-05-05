package com.aifomo.dashboard.collector.threads.session;

import java.nio.file.Path;
import java.util.Objects;

public record BrowserSessionDescriptor(
        BrowserSessionStatus status,
        Path profileDirectory,
        String message
) {

    public BrowserSessionDescriptor {
        Objects.requireNonNull(status, "status must not be null");
    }
}
