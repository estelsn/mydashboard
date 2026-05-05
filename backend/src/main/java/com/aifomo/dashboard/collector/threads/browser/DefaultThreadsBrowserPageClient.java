package com.aifomo.dashboard.collector.threads.browser;

import com.aifomo.dashboard.collector.threads.session.ThreadsBrowserSessionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DefaultThreadsBrowserPageClient implements ThreadsBrowserPageClient {

    private static final String LOGIN_MARKER = "로그인";
    private static final String ACCESS_RESTRICTED_MARKER = "이용할 수 없습니다";

    private final ThreadsBrowserSessionProperties sessionProperties;

    @Override
    public ThreadsBrowserPageSnapshot fetch(ThreadsBrowserPageRequest request) {
        String chromeExecutable = sessionProperties.getChromeExecutable();
        if (chromeExecutable == null || chromeExecutable.isBlank()) {
            return ThreadsBrowserPageSnapshot.failure(
                    ThreadsBrowserPageStatus.FAILED,
                    "Chrome executable path is not configured"
            );
        }

        List<String> command = buildCommand(chromeExecutable, request);
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean completed = process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.TIMEOUT, "Threads page render timed out");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.FAILED, firstLine(output, "Chrome exited with failure"));
            }
            if (output.isBlank()) {
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.EMPTY_RESULT, "Threads page returned an empty snapshot");
            }
            if (output.contains(ACCESS_RESTRICTED_MARKER)) {
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.ACCESS_RESTRICTED, "Threads page access is restricted");
            }
            if (output.contains(LOGIN_MARKER) && !output.contains("data-threads-post")) {
                return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.LOGIN_REQUIRED, "Threads login is required");
            }
            return ThreadsBrowserPageSnapshot.success(output);
        } catch (IOException ex) {
            return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.FAILED, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ThreadsBrowserPageSnapshot.failure(ThreadsBrowserPageStatus.FAILED, "Threads page render was interrupted");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static List<String> buildCommand(String chromeExecutable, ThreadsBrowserPageRequest request) {
        List<String> command = new ArrayList<>();
        command.add(chromeExecutable);
        command.add("--user-data-dir=" + request.profileDirectory().normalize());
        command.add("--disable-gpu");
        command.add("--no-sandbox");
        command.add("--virtual-time-budget=" + virtualTimeBudgetMillis(request.timeout(), request.maxScrollCount()));
        if (request.headless()) {
            command.add("--headless=new");
        }
        command.add("--dump-dom");
        command.add(request.url());
        return List.copyOf(command);
    }

    private static long virtualTimeBudgetMillis(Duration timeout, int maxScrollCount) {
        long scrollBudget = Math.max(1, maxScrollCount + 1) * 1_000L;
        return Math.min(timeout.toMillis(), scrollBudget);
    }

    private static String firstLine(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse(fallback);
    }
}
