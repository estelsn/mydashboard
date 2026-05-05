package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserProcessLauncher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aifomo-threads-session-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.threads.browser-session.profile-directory=./build/test-runtime/threads-session-api/missing-profile"
})
@AutoConfigureMockMvc
class ThreadsSessionApiTest {

    private static final Path TEST_PROFILE_DIRECTORY =
            Path.of("./build/test-runtime/threads-session-api/missing-profile");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ThreadsLoginBrowserProcessLauncher threadsLoginBrowserProcessLauncher;

    @BeforeEach
    void resetProfileDirectory() throws Exception {
        if (Files.exists(TEST_PROFILE_DIRECTORY)) {
            try (var paths = Files.walk(TEST_PROFILE_DIRECTORY)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException exception) {
                                throw new IllegalStateException(exception);
                            }
                        });
            }
        }
    }

    @Test
    void exposesThreadsSessionStatusWithoutSensitiveData() throws Exception {
        mockMvc.perform(get("/api/threads/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOGIN_REQUIRED"))
                .andExpect(jsonPath("$.profilePath").value("build/test-runtime/threads-session-api/missing-profile"))
                .andExpect(jsonPath("$.profileDirectory").doesNotExist())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.cookies").doesNotExist())
                .andExpect(jsonPath("$.tokens").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void opensThreadsLoginBrowserWithoutSensitiveData() throws Exception {
        mockMvc.perform(post("/api/threads/session/open-login"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("OPENED"))
                .andExpect(jsonPath("$.profilePath").value("build/test-runtime/threads-session-api/missing-profile"))
                .andExpect(jsonPath("$.loginUrl").value("https://www.threads.net/"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").doesNotExist())
                .andExpect(jsonPath("$.cookies").doesNotExist())
                .andExpect(jsonPath("$.tokens").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void allowsLocalFrontendCorsPreflightForOpenLogin() throws Exception {
        mockMvc.perform(options("/api/threads/session/open-login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void returnsClearErrorWhenChromeLaunchFails() throws Exception {
        doThrow(new IOException("chrome missing")).when(threadsLoginBrowserProcessLauncher).launch(anyList());

        mockMvc.perform(post("/api/threads/session/open-login"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Threads login browser launch failed"))
                .andExpect(jsonPath("$.detail").value("Failed to open Chrome for Threads login"))
                .andExpect(jsonPath("$.command").doesNotExist())
                .andExpect(jsonPath("$.cookies").doesNotExist())
                .andExpect(jsonPath("$.tokens").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
