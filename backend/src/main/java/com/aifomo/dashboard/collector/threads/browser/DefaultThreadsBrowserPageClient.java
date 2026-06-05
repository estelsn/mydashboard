package com.aifomo.dashboard.collector.threads.browser;

import com.aifomo.dashboard.collector.threads.session.ThreadsBrowserSessionProperties;
import org.springframework.stereotype.Component;

@Component
public class DefaultThreadsBrowserPageClient implements ThreadsBrowserPageClient {

    private static final String LOGIN_MARKER = "로그인";
    private static final String ACCESS_RESTRICTED_MARKER = "이용할 수 없습니다";

    private final ThreadsBrowserSessionProperties sessionProperties;
    private final ThreadsBrowserAutomation browserAutomation;

    DefaultThreadsBrowserPageClient(
            ThreadsBrowserSessionProperties sessionProperties,
            ThreadsBrowserAutomation browserAutomation
    ) {
        this.sessionProperties = sessionProperties;
        this.browserAutomation = browserAutomation;
    }

    @Override
    public ThreadsBrowserPageSnapshot fetch(ThreadsBrowserPageRequest request) {
        String chromeExecutable = sessionProperties.getChromeExecutable();
        if (chromeExecutable == null || chromeExecutable.isBlank()) {
            return ThreadsBrowserPageSnapshot.failure(
                    ThreadsBrowserPageStatus.FAILED,
                    "Chrome executable path is not configured"
            );
        }

        try {
            String output = browserAutomation.fetchRenderedContent(chromeExecutable, request);
            if (output.isBlank()) {
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.EMPTY_RESULT, "Threads 페이지가 비어 있습니다.");
            }
            if (output.contains(ACCESS_RESTRICTED_MARKER)) {
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.ACCESS_RESTRICTED, "Threads 접근이 제한되었습니다.");
            }
            if (output.contains(LOGIN_MARKER) && !output.contains("data-threads-post")) {
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.LOGIN_REQUIRED, "Threads 로그인이 필요합니다.");
            }
            return ThreadsBrowserPageSnapshot.success(output);
        } catch (java.util.concurrent.TimeoutException ex) {
            return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.TIMEOUT, "Threads 페이지 로딩 시간이 초과되었습니다.");
        } catch (Exception ex) {
            return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.FAILED, ex.getMessage());
        }
    }
}
