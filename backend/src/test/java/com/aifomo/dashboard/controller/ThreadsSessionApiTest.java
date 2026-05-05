package com.aifomo.dashboard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aifomo-threads-session-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.threads.browser-session.profile-directory=./build/test-runtime/threads-session-api/missing-profile"
})
@AutoConfigureMockMvc
class ThreadsSessionApiTest {

    @Autowired
    private MockMvc mockMvc;

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
}
