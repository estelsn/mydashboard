package com.aifomo.dashboard.collector.threads.browser;

public interface ThreadsBrowserAutomation {

    ThreadsBrowserRenderResult fetchRenderedContent(
            String chromeExecutable,
            ThreadsBrowserPageRequest request
    ) throws Exception;
}
