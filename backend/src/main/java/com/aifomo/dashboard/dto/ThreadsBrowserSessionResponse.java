package com.aifomo.dashboard.dto;

import com.aifomo.dashboard.collector.threads.session.BrowserSessionDescriptor;
import com.aifomo.dashboard.collector.threads.session.BrowserSessionStatus;

public record ThreadsBrowserSessionResponse(
        BrowserSessionStatus status,
        String profileDirectory,
        String message
) {

    public static ThreadsBrowserSessionResponse from(BrowserSessionDescriptor descriptor) {
        return new ThreadsBrowserSessionResponse(
                descriptor.status(),
                descriptor.profileDirectory() == null ? null : descriptor.profileDirectory().toString(),
                descriptor.message()
        );
    }
}
