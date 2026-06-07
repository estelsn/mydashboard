package com.aifomo.dashboard.collector.threads.browser;

import com.aifomo.dashboard.collector.threads.session.ThreadsBrowserSessionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DefaultThreadsBrowserPageClient implements ThreadsBrowserPageClient {

    private static final String ACCESS_RESTRICTED_MARKER = "이용할 수 없습니다";
    private static final String PROFILE_LOCK_MESSAGE = "Threads 브라우저 프로필이 다른 Chrome 인스턴스에서 사용 중입니다. Threads 관련 Chrome 창을 닫고 다시 시도하세요.";
    private static final String GENERIC_BROWSER_CLOSED_MESSAGE = "Threads 브라우저가 예기치 않게 종료되었습니다. 잠시 후 다시 시도하세요.";

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
            ThreadsBrowserRenderResult renderResult = browserAutomation.fetchRenderedContent(chromeExecutable, request);
            String output = renderResult.content();
            if (output.isBlank()) {
                log.info("Threads page fetch result: url={}, loggedIn=false, status=EMPTY_RESULT", request.url());
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.EMPTY_RESULT, "Threads 페이지가 비어 있습니다.");
            }
            if (output.contains(ACCESS_RESTRICTED_MARKER)) {
                log.info("Threads page fetch result: url={}, loggedIn=false, status=ACCESS_RESTRICTED", request.url());
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.ACCESS_RESTRICTED, "Threads 접근이 제한되었습니다.");
            }
            if (!renderResult.loggedIn()) {
                log.info("Threads page fetch result: url={}, loggedIn=false, status=LOGIN_REQUIRED", request.url());
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.LOGIN_REQUIRED, "Threads 로그인이 필요합니다.");
            }
            log.info("Threads page fetch result: url={}, loggedIn=true, status=SUCCESS", request.url());
            return ThreadsBrowserPageSnapshot.success(output);
        } catch (java.util.concurrent.TimeoutException ex) {
            return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.TIMEOUT, "Threads 페이지 로딩 시간이 초과되었습니다.");
        } catch (Exception ex) {
            return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.FAILED, sanitizeFailureMessage(ex));
        }
    }

    private String sanitizeFailureMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return GENERIC_BROWSER_CLOSED_MESSAGE;
        }

        if (containsProfileLockMarker(message)) {
            return PROFILE_LOCK_MESSAGE;
        }
        if (message.contains("Target page, context or browser has been closed")) {
            return GENERIC_BROWSER_CLOSED_MESSAGE;
        }
        return message;
    }

    private boolean containsProfileLockMarker(String message) {
        return message.contains("SingletonLock")
                || message.contains("ProcessSingleton")
                || message.contains("profile is locked by another Chrome process")
                || message.contains("profile directory. This means that running multiple instances");
    }
}
