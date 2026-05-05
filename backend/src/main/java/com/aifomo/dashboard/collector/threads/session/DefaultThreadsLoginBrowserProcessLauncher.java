package com.aifomo.dashboard.collector.threads.session;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class DefaultThreadsLoginBrowserProcessLauncher implements ThreadsLoginBrowserProcessLauncher {

    @Override
    public void launch(List<String> command) throws IOException {
        new ProcessBuilder(command).start();
    }
}
