package com.aifomo.dashboard.collector.threads.session;

import java.nio.file.Path;
import java.util.List;

public record ThreadsLoginBrowserCommand(
        List<String> command,
        Path profileDirectory,
        String loginUrl
) {

    public ThreadsLoginBrowserCommand {
        command = List.copyOf(command);
    }
}
