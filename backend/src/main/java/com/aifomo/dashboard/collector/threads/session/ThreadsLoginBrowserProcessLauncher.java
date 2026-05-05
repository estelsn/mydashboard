package com.aifomo.dashboard.collector.threads.session;

import java.io.IOException;
import java.util.List;

public interface ThreadsLoginBrowserProcessLauncher {

    void launch(List<String> command) throws IOException;
}
