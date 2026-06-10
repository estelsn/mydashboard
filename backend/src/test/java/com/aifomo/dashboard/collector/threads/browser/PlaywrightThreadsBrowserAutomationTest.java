package com.aifomo.dashboard.collector.threads.browser;

import com.aifomo.dashboard.collector.threads.ThreadsCollectionProperties;
import com.aifomo.dashboard.collector.threads.session.ThreadsBrowserSessionProperties;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlaywrightThreadsBrowserAutomationTest {

    @TempDir
    Path tempDir;

    @Test
    void performsEveryRequestedScrollWithConfiguredDelay() {
        ThreadsCollectionProperties properties = new ThreadsCollectionProperties();
        properties.getSafety().setDelayBetweenScrolls(Duration.ofSeconds(3));
        PlaywrightThreadsBrowserAutomation automation = new PlaywrightThreadsBrowserAutomation(
                properties,
                new ThreadsBrowserSessionProperties()
        );
        Page page = mock(Page.class);

        int performedScrolls = automation.performScrolling(page, 5, Duration.ofSeconds(45));

        assertThat(performedScrolls).isEqualTo(5);
        verify(page, times(5)).evaluate("window.scrollTo(0, document.body.scrollHeight)");
        verify(page, times(5)).waitForTimeout(3_000);
    }

    @Test
    void skipsScrollingWhenRequestIsZero() {
        PlaywrightThreadsBrowserAutomation automation = new PlaywrightThreadsBrowserAutomation(
                new ThreadsCollectionProperties(),
                new ThreadsBrowserSessionProperties()
        );
        Page page = mock(Page.class);

        assertThat(automation.performScrolling(page, 0, Duration.ofSeconds(45))).isZero();
        verifyNoInteractions(page);
    }

    @Test
    void passesNormalizedRequestedAccountToStructuredExtraction() {
        PlaywrightThreadsBrowserAutomation automation = automation();
        Page page = mock(Page.class);
        when(page.evaluate(anyString(), eq("choi.openai"))).thenReturn("[{\"postUrl\":\"canonical\"}]");

        String result = automation.extractStructuredPosts(
                page,
                "https://www.threads.com/@Choi.OpenAI/"
        );

        assertThat(result).isEqualTo("[{\"postUrl\":\"canonical\"}]");
        verify(page).evaluate(anyString(), eq("choi.openai"));
    }

    @Test
    void extractsAccountHandleFromThreadsProfileUrl() {
        assertThat(PlaywrightThreadsBrowserAutomation.expectedAccountHandle(
                "https://www.threads.com/@Specal1849/"
        )).isEqualTo("specal1849");
    }

    @Test
    void acceptsAuthenticatedThreadsUiWithoutLegacySessionCookie() {
        PlaywrightThreadsBrowserAutomation automation = automation();
        BrowserContext context = mock(BrowserContext.class);
        when(context.cookies(anyList())).thenReturn(List.of());

        PlaywrightThreadsBrowserAutomation.AuthenticationSignals signals =
                automation.authenticationSignals(context, "Activity", "<a href=\"/activity\">Activity</a>");

        assertThat(signals.loggedIn()).isTrue();
        assertThat(signals.hasAuthCookie()).isFalse();
        assertThat(signals.hasAuthenticatedUi()).isTrue();
        assertThat(signals.loginPage()).isFalse();
    }

    @Test
    void rejectsLoginPageEvenWhenLegacySessionCookieExists() {
        PlaywrightThreadsBrowserAutomation automation = automation();
        BrowserContext context = mock(BrowserContext.class);
        Cookie cookie = new Cookie("sessionid", "redacted");
        cookie.domain = ".threads.com";
        when(context.cookies(anyList())).thenReturn(List.of(cookie));

        PlaywrightThreadsBrowserAutomation.AuthenticationSignals signals =
                automation.authenticationSignals(
                        context,
                        "Continue with Instagram",
                        "<button>Continue with Instagram</button>"
                );

        assertThat(signals.loggedIn()).isFalse();
        assertThat(signals.hasAuthCookie()).isTrue();
        assertThat(signals.loginPage()).isTrue();
    }

    @Test
    void rejectsPageWithoutAuthenticationEvidence() {
        PlaywrightThreadsBrowserAutomation automation = automation();
        BrowserContext context = mock(BrowserContext.class);
        when(context.cookies(anyList())).thenReturn(List.of());

        PlaywrightThreadsBrowserAutomation.AuthenticationSignals signals =
                automation.authenticationSignals(context, "Public profile", "<main>Public profile</main>");

        assertThat(signals.loggedIn()).isFalse();
        assertThat(signals.hasAuthenticatedUi()).isFalse();
        assertThat(signals.loginPage()).isFalse();
    }

    @Test
    void ignoresLoginTextThatIsNotVisible() {
        PlaywrightThreadsBrowserAutomation automation = automation();
        BrowserContext context = mock(BrowserContext.class);
        Cookie cookie = new Cookie("sessionid", "redacted");
        cookie.domain = ".threads.com";
        when(context.cookies(anyList())).thenReturn(List.of(cookie));

        PlaywrightThreadsBrowserAutomation.AuthenticationSignals signals =
                automation.authenticationSignals(
                        context,
                        "Activity",
                        "<a href=\"/activity\">Activity</a><template>Continue with Instagram</template>"
                );

        assertThat(signals.loggedIn()).isTrue();
        assertThat(signals.loginPage()).isFalse();
    }

    @Test
    void ignoresStaleChromeSingletonLock() throws Exception {
        Path lock = tempDir.resolve("SingletonLock");
        Files.createSymbolicLink(lock, Path.of("localhost-999999999"));

        assertThat(automation().isProfileLocked(tempDir)).isFalse();
    }

    @Test
    void detectsActiveChromeSingletonLock() throws Exception {
        Path lock = tempDir.resolve("SingletonLock");
        Files.createSymbolicLink(lock, Path.of("localhost-" + ProcessHandle.current().pid()));

        assertThat(automation().isProfileLocked(tempDir)).isTrue();
    }

    private PlaywrightThreadsBrowserAutomation automation() {
        return new PlaywrightThreadsBrowserAutomation(
                new ThreadsCollectionProperties(),
                new ThreadsBrowserSessionProperties()
        );
    }
}
