package com.aifomo.dashboard.collector.threads.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Component
public class PlaywrightThreadsBrowserAutomation implements ThreadsBrowserAutomation {

    private static final double SCROLL_WAIT_MILLIS = 2_500;
    private static final int MAX_STALLED_SCROLLS = 2;

    @Override
    public String fetchRenderedContent(String chromeExecutable, ThreadsBrowserPageRequest request) throws Exception {
        Path profileDirectory = request.profileDirectory().normalize();
        Files.createDirectories(profileDirectory);

        try (Playwright playwright = Playwright.create();
             BrowserContext context = playwright.chromium().launchPersistentContext(
                     profileDirectory,
                     launchOptions(chromeExecutable, request)
             )) {
            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().getFirst();
            page.navigate(request.url(), new Page.NavigateOptions()
                    .setTimeout(request.timeout().toMillis())
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(1_000);
            performScrolling(page, request.maxScrollCount(), request.timeout());
            return page.content();
        }
    }

    private BrowserType.LaunchPersistentContextOptions launchOptions(String chromeExecutable, ThreadsBrowserPageRequest request) {
        return new BrowserType.LaunchPersistentContextOptions()
                .setExecutablePath(Path.of(chromeExecutable))
                .setHeadless(request.headless())
                .setArgs(List.of("--disable-gpu", "--no-sandbox"));
    }

    private void performScrolling(Page page, int maxScrollCount, Duration timeout) {
        if (maxScrollCount <= 0) {
            return;
        }

        page.setDefaultTimeout(Math.max(1_000, timeout.toMillis()));
        int previousPostCount = articleCount(page);
        int stalledScrolls = 0;

        for (int index = 0; index < maxScrollCount; index++) {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
            page.waitForTimeout(SCROLL_WAIT_MILLIS);

            int currentPostCount = articleCount(page);
            if (currentPostCount > previousPostCount) {
                previousPostCount = currentPostCount;
                stalledScrolls = 0;
                continue;
            }

            stalledScrolls++;
            if (stalledScrolls >= MAX_STALLED_SCROLLS) {
                return;
            }
        }
    }

    private int articleCount(Page page) {
        Object count = page.evaluate("document.querySelectorAll('article[data-threads-post]').length");
        if (count instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
