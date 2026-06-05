package com.aifomo.dashboard.collector.threads.browser;

import com.aifomo.dashboard.collector.threads.session.ThreadsBrowserSessionProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultThreadsBrowserPageClientTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsSuccessWhenAutomationProvidesRenderedDom() {
        ThreadsBrowserPageSnapshot snapshot = client((chromeExecutable, request) -> """
                <article data-threads-post data-author="@choi.openai">
                  <div>Rendered</div>
                </article>
                """).fetch(request());

        assertThat(snapshot.status()).isEqualTo(ThreadsBrowserPageStatus.SUCCESS);
        assertThat(snapshot.rawContent()).contains("Rendered");
    }

    @Test
    void mapsLoginAndAccessRestrictedMarkers() {
        ThreadsBrowserPageSnapshot loginRequired = client((chromeExecutable, request) -> "<div>로그인</div>")
                .fetch(request());
        ThreadsBrowserPageSnapshot restricted = client((chromeExecutable, request) -> "<div>이용할 수 없습니다</div>")
                .fetch(request());

        assertThat(loginRequired.status()).isEqualTo(ThreadsBrowserPageStatus.LOGIN_REQUIRED);
        assertThat(restricted.status()).isEqualTo(ThreadsBrowserPageStatus.ACCESS_RESTRICTED);
    }

    @Test
    void mapsTimeoutExceptionToTimeoutStatus() {
        ThreadsBrowserPageSnapshot snapshot = client((chromeExecutable, request) -> {
            throw new TimeoutException("timeout");
        }).fetch(request());

        assertThat(snapshot.status()).isEqualTo(ThreadsBrowserPageStatus.TIMEOUT);
    }

    @Test
    void sanitizesProfileLockFailureMessage() {
        ThreadsBrowserPageSnapshot snapshot = client((chromeExecutable, request) -> {
            throw new IllegalStateException("Failed to create /tmp/threads/SingletonLock: File exists (17)");
        }).fetch(request());

        assertThat(snapshot.status()).isEqualTo(ThreadsBrowserPageStatus.FAILED);
        assertThat(snapshot.message()).isEqualTo("Threads 브라우저 프로필이 다른 Chrome 인스턴스에서 사용 중입니다. Threads 관련 Chrome 창을 닫고 다시 시도하세요.");
    }

    private DefaultThreadsBrowserPageClient client(ThreadsBrowserAutomation automation) {
        ThreadsBrowserSessionProperties properties = new ThreadsBrowserSessionProperties();
        properties.setChromeExecutable("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        return new DefaultThreadsBrowserPageClient(properties, automation);
    }

    private ThreadsBrowserPageRequest request() {
        return new ThreadsBrowserPageRequest(
                "https://www.threads.com/@choi.openai",
                tempDir,
                true,
                4,
                Duration.ofSeconds(30)
        );
    }
}
