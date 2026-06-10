package com.aifomo.dashboard.collector.threads.browser;

import com.aifomo.dashboard.collector.threads.ThreadsCollectionProperties;
import com.aifomo.dashboard.collector.threads.session.ThreadsBrowserSessionProperties;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlaywrightThreadsBrowserAutomation implements ThreadsBrowserAutomation {

    private static final String CHROME_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/148.0.0.0 Safari/537.36";
    private static final String EXTRACT_POSTS_SCRIPT = """
            (expectedHandle) => {
              const normalize = value => (value || '').replace(/\\s+/g, ' ').trim();
              const postLinks = Array.from(document.querySelectorAll('a[href*="/post/"]'));
              const seen = new Set();
              const posts = [];

              for (const link of postLinks) {
                const parsedUrl = new URL(link.href, location.origin);
                const postMatch = parsedUrl.pathname.match(/^\\/@([^/]+)\\/post\\/([^/]+)/i);
                if (!postMatch) continue;

                const authorHandle = postMatch[1];
                if (expectedHandle && authorHandle.toLowerCase() !== expectedHandle.toLowerCase()) continue;

                const postUrl = new URL(`/@${authorHandle}/post/${postMatch[2]}`, parsedUrl.origin).href;
                if (seen.has(postUrl)) continue;
                seen.add(postUrl);

                const authorIdentifier = '@' + authorHandle;
                let container = link;
                let postContainer = null;
                let fallbackContainer = null;

                for (let depth = 0; depth < 12 && container; depth++, container = container.parentElement) {
                  const text = normalize(container.innerText);
                  const linkCount = container.querySelectorAll('a').length;
                  if (text.length >= 20 && text.length <= 6000 && linkCount >= 2) {
                    fallbackContainer ||= container;
                    if (container.querySelector('time')) {
                      postContainer = container;
                      break;
                    }
                  }
                }
                postContainer ||= fallbackContainer;
                if (!postContainer) continue;

                const timeElement = postContainer.querySelector('time');
                const timestampLink = Array.from(postContainer.querySelectorAll('a[href*="/post/"]'))
                  .find(candidate => {
                    const candidateUrl = new URL(candidate.href, location.origin);
                    const candidateMatch = candidateUrl.pathname.match(/^\\/@([^/]+)\\/post\\/([^/]+)/i);
                    return candidateMatch
                      && candidateMatch[1].toLowerCase() === authorHandle.toLowerCase()
                      && candidateMatch[2] === postMatch[2]
                      && normalize(candidate.innerText);
                  });
                const displayTime = timeElement
                  ? normalize(timeElement.getAttribute('datetime') || timeElement.innerText)
                  : normalize(timestampLink ? timestampLink.innerText : link.innerText);
                const ignored = new Set([
                  normalize(authorIdentifier),
                  normalize(authorIdentifier && authorIdentifier.substring(1)),
                  displayTime,
                  '좋아요',
                  '답글',
                  '리포스트',
                  '공유',
                  'Like',
                  'Reply',
                  'Repost',
                  'Share'
                ]);
                const bodyCandidates = Array.from(postContainer.querySelectorAll('[dir="auto"]'))
                  .map(element => normalize(element.innerText))
                  .filter(text => text.length >= 2 && !ignored.has(text))
                  .filter(text => !/^\\d+[smhdw]$/i.test(text))
                  .filter((text, index, values) => values.indexOf(text) === index)
                  .sort((left, right) => right.length - left.length);
                const body = bodyCandidates[0] || normalize(postContainer.innerText);
                if (!body) continue;

                posts.push({
                  authorIdentifier,
                  body,
                  postUrl,
                  displayTime: displayTime || null
                });
              }
              return JSON.stringify(posts);
            }
            """;
    private static final Set<String> PROFILE_LOCK_FILES = Set.of(
            "SingletonLock",
            "SingletonCookie",
            "SingletonSocket",
            "lockfile"
    );
    private static final Set<String> AUTH_COOKIE_NAMES = Set.of("sessionid");
    private static final Set<String> AUTHENTICATED_UI_MARKERS = Set.of(
            "href=\"/activity",
            "href=\"https://www.threads.com/activity",
            "href=\"/settings",
            "href=\"https://www.threads.com/settings"
    );

    private final ThreadsCollectionProperties collectionProperties;
    private final ThreadsBrowserSessionProperties sessionProperties;

    @Override
    public ThreadsBrowserRenderResult fetchRenderedContent(
            String chromeExecutable,
            ThreadsBrowserPageRequest request
    ) throws Exception {
        Path profileDirectory = request.profileDirectory().normalize().toAbsolutePath();
        Files.createDirectories(profileDirectory);
        String profileName = sessionProperties.getProfileName();
        List<String> chromeArguments = List.of(
                "--user-data-dir=" + profileDirectory,
                "--profile-directory=" + profileName,
                "--restore-last-session",
                "--disable-blink-features=AutomationControlled",
                "--disable-gpu",
                "--no-sandbox"
        );
        log.info("Threads browser launch: mode={}, userDataDir={}, url={}, headless={}, maxScrollCount={}, args={}, ignoredDefaultArgs={}",
                "persistent-profile",
                profileDirectory,
                request.url(),
                request.headless(),
                request.maxScrollCount(),
                chromeArguments,
                List.of("--enable-automation", "--use-mock-keychain"));
        if (isProfileLocked(profileDirectory)) {
            throw new IllegalStateException(
                    "Threads profile is locked by another Chrome process. "
                            + "Close the Threads login browser before collecting: "
                            + profileDirectory
            );
        }

        try (Playwright playwright = Playwright.create();
             BrowserContext context = playwright.chromium().launchPersistentContext(
                     profileDirectory,
                     launchOptions(chromeExecutable, request, profileName)
             )) {
            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().getFirst();
            return renderPage(page, request);
        }
    }

    private ThreadsBrowserRenderResult renderPage(Page page, ThreadsBrowserPageRequest request) {
        boolean authCookieBeforeNavigation = hasAuthCookie(page.context());
        try {
            page.navigate(request.url(), new Page.NavigateOptions()
                    .setTimeout(request.timeout().toMillis())
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        } catch (RuntimeException exception) {
            log.warn("Threads browser failure: url={}, stage=page-access, message={}",
                    request.url(),
                    exception.getMessage());
            throw exception;
        }
        page.waitForTimeout(3_000);
        int beforeCount = articleCount(page);
        String initialContent = page.content();
        int beforeLength = initialContent.length();
        AuthenticationSignals authentication = authenticationSignals(
                page.context(),
                visiblePageText(page),
                initialContent
        );
        boolean loggedIn = authentication.loggedIn();
        log.info("Threads authentication check: url={}, loggedIn={}, authCookieBeforeNavigation={}, authCookieAfterNavigation={}, authenticatedUi={}, loginPage={}",
                request.url(),
                loggedIn,
                authCookieBeforeNavigation,
                authentication.hasAuthCookie(),
                authentication.hasAuthenticatedUi(),
                authentication.loginPage());
        if (!loggedIn) {
            log.info("Threads scroll metrics: url={}, requestedScrolls={}, performedScrolls=0, beforeCandidates={}, afterCandidates={}, beforeDomLength={}, afterDomLength={}",
                    request.url(),
                    request.maxScrollCount(),
                    beforeCount,
                    beforeCount,
                    beforeLength,
                    beforeLength);
            return new ThreadsBrowserRenderResult(initialContent, false);
        }

        int performedScrolls;
        try {
            performedScrolls = performScrolling(page, request.maxScrollCount(), request.timeout());
        } catch (RuntimeException exception) {
            log.warn("Threads browser failure: url={}, stage=scroll, message={}",
                    request.url(),
                    exception.getMessage());
            throw exception;
        }
        int afterCount = articleCount(page);
        String content;
        String structuredContent;
        try {
            content = page.content();
            structuredContent = extractStructuredPosts(page, request.url());
        } catch (RuntimeException exception) {
            log.warn("Threads browser failure: url={}, stage=dom-collection, message={}",
                    request.url(),
                    exception.getMessage());
            throw exception;
        }
        log.info("Threads scroll metrics: url={}, requestedScrolls={}, performedScrolls={}, beforeCandidates={}, afterCandidates={}, beforeDomLength={}, afterDomLength={}",
                request.url(),
                request.maxScrollCount(),
                performedScrolls,
                beforeCount,
                afterCount,
                beforeLength,
                content.length());
        log.info("Threads structured extraction: url={}, extractedPostCount={}",
                request.url(),
                structuredPostCount(structuredContent));
        return new ThreadsBrowserRenderResult(structuredContent, true);
    }

    private BrowserType.LaunchPersistentContextOptions launchOptions(
            String chromeExecutable,
            ThreadsBrowserPageRequest request,
            String profileName
    ) {
        return new BrowserType.LaunchPersistentContextOptions()
                .setExecutablePath(Path.of(chromeExecutable))
                .setHeadless(request.headless())
                .setUserAgent(CHROME_USER_AGENT)
                .setIgnoreDefaultArgs(List.of(
                        "--enable-automation",
                        "--use-mock-keychain"
                ))
                .setArgs(List.of(
                        "--profile-directory=" + profileName,
                        "--restore-last-session",
                        "--disable-blink-features=AutomationControlled",
                        "--disable-gpu",
                        "--no-sandbox"
                ));
    }

    int performScrolling(Page page, int maxScrollCount, Duration timeout) {
        if (maxScrollCount <= 0) {
            return 0;
        }

        page.setDefaultTimeout(Math.max(1_000, timeout.toMillis()));
        int performedScrolls = 0;
        double scrollWaitMillis = Math.max(2_000, collectionProperties.getSafety().getDelayBetweenScrolls().toMillis());

        for (int index = 0; index < maxScrollCount; index++) {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
            performedScrolls++;
            page.waitForTimeout(scrollWaitMillis);
        }
        return performedScrolls;
    }

    private int articleCount(Page page) {
        Object count = page.evaluate("new Set(Array.from(document.querySelectorAll('a[href*=\"/post/\"]')).map(link => link.href)).size");
        if (count instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    String extractStructuredPosts(Page page, String profileUrl) {
        Object result = page.evaluate(EXTRACT_POSTS_SCRIPT, expectedAccountHandle(profileUrl));
        return result instanceof String value ? value : "[]";
    }

    static String expectedAccountHandle(String profileUrl) {
        String path = URI.create(profileUrl).getPath();
        if (path == null || path.length() < 3 || !path.startsWith("/@")) {
            throw new IllegalArgumentException("Threads profile URL must contain an account handle");
        }
        int slash = path.indexOf('/', 2);
        String handle = slash < 0 ? path.substring(2) : path.substring(2, slash);
        if (handle.isBlank()) {
            throw new IllegalArgumentException("Threads profile URL must contain an account handle");
        }
        return handle.toLowerCase(Locale.ROOT);
    }

    private int structuredPostCount(String structuredContent) {
        if (structuredContent == null || structuredContent.length() < 2) {
            return 0;
        }
        int count = 0;
        int offset = 0;
        while ((offset = structuredContent.indexOf("\"postUrl\"", offset)) >= 0) {
            count++;
            offset += 9;
        }
        return count;
    }

    boolean isProfileLocked(Path profileDirectory) {
        Path singletonLock = profileDirectory.resolve("SingletonLock");
        if (Files.exists(singletonLock, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isSymbolicLink(singletonLock)) {
                return true;
            }
            try {
                String lockOwner = Files.readSymbolicLink(singletonLock).getFileName().toString();
                int separator = lockOwner.lastIndexOf('-');
                if (separator >= 0 && separator + 1 < lockOwner.length()) {
                    long processId = Long.parseLong(lockOwner.substring(separator + 1));
                    if (ProcessHandle.of(processId).map(ProcessHandle::isAlive).orElse(false)) {
                        return true;
                    }
                }
            } catch (Exception exception) {
                log.warn("Unable to inspect Threads Chrome profile lock: path={}, message={}",
                        singletonLock,
                        exception.getMessage());
                return true;
            }
        }

        return PROFILE_LOCK_FILES.stream()
                .filter(fileName -> !"SingletonLock".equals(fileName))
                .map(profileDirectory::resolve)
                .anyMatch(Files::exists);
    }

    AuthenticationSignals authenticationSignals(
            BrowserContext context,
            String visibleText,
            String content
    ) {
        boolean hasAuthCookies = hasAuthCookie(context);
        String normalizedContent = content.toLowerCase();
        boolean loginPage = looksLikeLoginPage(visibleText.toLowerCase());
        boolean hasAuthenticatedUi = AUTHENTICATED_UI_MARKERS.stream().anyMatch(normalizedContent::contains);
        return new AuthenticationSignals(
                !loginPage && (hasAuthCookies || hasAuthenticatedUi),
                hasAuthCookies,
                hasAuthenticatedUi,
                loginPage
        );
    }

    private boolean hasAuthCookie(BrowserContext context) {
        return context.cookies(List.of(
                        "https://www.threads.com/",
                        "https://www.instagram.com/"
                )).stream()
                .filter(this::isThreadsCookie)
                .map(cookie -> cookie.name)
                .map(String::toLowerCase)
                .anyMatch(AUTH_COOKIE_NAMES::contains);
    }

    private boolean isThreadsCookie(Cookie cookie) {
        String domain = cookie.domain == null ? "" : cookie.domain.toLowerCase();
        return domain.contains("threads.") || domain.contains("instagram.");
    }

    private String visiblePageText(Page page) {
        Object text = page.evaluate("document.body ? document.body.innerText : ''");
        return text instanceof String value ? value : "";
    }

    private boolean looksLikeLoginPage(String normalizedVisibleText) {
        return normalizedVisibleText.contains("instagram으로 계속하기")
                || normalizedVisibleText.contains("continue with instagram")
                || normalizedVisibleText.contains("log in with instagram")
                || normalizedVisibleText.contains("threads에서 소통해보세요");
    }

    record AuthenticationSignals(
            boolean loggedIn,
            boolean hasAuthCookie,
            boolean hasAuthenticatedUi,
            boolean loginPage
    ) {
    }
}
