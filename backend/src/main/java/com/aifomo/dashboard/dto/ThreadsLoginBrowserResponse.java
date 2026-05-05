package com.aifomo.dashboard.dto;

public record ThreadsLoginBrowserResponse(
        String status,
        String profilePath,
        String loginUrl,
        String message
) {
}
