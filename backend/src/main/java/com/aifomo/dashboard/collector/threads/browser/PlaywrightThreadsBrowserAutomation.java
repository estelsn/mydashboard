package com.aifomo.dashboard.collector.threads.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class PlaywrightThreadsBrowserAutomation implements ThreadsBrowserAutomation {

    private static final double SCROLL_WAIT_MILLIS = 2_500;
    private static final int MAX_STALLED_SCROLLS = 2;
    private static final Set<String> TRANSIENT_PROFILE_FILES = Set.of(
            "SingletonLock",
            "SingletonCookie",
            "SingletonSocket",
            "lockfile"
    );

    @Override
    public String fetchRenderedContent(String chromeExecutable, ThreadsBrowserPageRequest request) throws Exception {
        Path profileDirectory = request.profileDirectory().normalize().toAbsolutePath();
        prepareSourceProfileDirectory(profileDirectory);
        Path temporaryProfileDirectory = createTemporaryProfileCopy(profileDirectory);

        try {
            try (Playwright playwright = Playwright.create();
                 BrowserContext context = playwright.chromium().launchPersistentContext(
                         temporaryProfileDirectory,
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
        } finally {
            deleteDirectoryQuietly(temporaryProfileDirectory);
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

    private void prepareSourceProfileDirectory(Path profileDirectory) throws IOException {
        Files.createDirectories(profileDirectory);
        deleteTransientProfileEntries(profileDirectory);
    }

    private Path createTemporaryProfileCopy(Path sourceDirectory) throws IOException {
        Path temporaryProfileDirectory = Files.createTempDirectory("threads-playwright-profile-");
        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            for (Path sourcePath : paths.toList()) {
                Path relativePath = sourceDirectory.relativize(sourcePath);
                if (relativePath.toString().isBlank() || containsTransientProfileEntry(relativePath)) {
                    continue;
                }

                Path targetPath = temporaryProfileDirectory.resolve(relativePath.toString());
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                    continue;
                }

                Files.createDirectories(targetPath.getParent());
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
        return temporaryProfileDirectory;
    }

    private void deleteTransientProfileEntries(Path profileDirectory) throws IOException {
        for (String fileName : TRANSIENT_PROFILE_FILES) {
            Files.deleteIfExists(profileDirectory.resolve(fileName));
        }
    }

    private boolean containsTransientProfileEntry(Path relativePath) {
        for (Path segment : relativePath) {
            if (TRANSIENT_PROFILE_FILES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private void deleteDirectoryQuietly(Path directory) {
        if (directory == null || Files.notExists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
