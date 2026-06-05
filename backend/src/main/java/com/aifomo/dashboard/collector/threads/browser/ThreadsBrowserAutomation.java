package com.aifomo.dashboard.collector.threads.browser;

public interface ThreadsBrowserAutomation {

    String fetchRenderedContent(String chromeExecutable, ThreadsBrowserPageRequest request) throws Exception;
}
