package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aifomo-collection-run-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CollectionRunApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollectionRunRepository collectionRunRepository;

    @Test
    void exposesRecentCollectionRunsForStatusDisplay() throws Exception {
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 1, 10, 0);
        collectionRunRepository.save(new CollectionRun(
                CollectionRunStatus.SUCCEEDED,
                12,
                11,
                1,
                42,
                startedAt,
                startedAt.plusMinutes(3),
                "Completed with one source failure"
        ));

        mockMvc.perform(get("/api/collection-runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$[0].totalSourceCount").value(12))
                .andExpect(jsonPath("$[0].successfulSourceCount").value(11))
                .andExpect(jsonPath("$[0].failedSourceCount").value(1))
                .andExpect(jsonPath("$[0].collectedItemCount").value(42))
                .andExpect(jsonPath("$[0].statusMessage").value("Completed with one source failure"));
    }
}
