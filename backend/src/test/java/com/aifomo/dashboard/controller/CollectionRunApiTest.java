package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.collector.threads.ThreadsCollectedPost;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionResult;
import com.aifomo.dashboard.collector.threads.ThreadsCollectionStatus;
import com.aifomo.dashboard.collector.threads.ThreadsCollector;
import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aifomo-collection-run-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.threads.collection.safety.min-source-recollection-interval=0s",
        "app.threads.collection.safety.delay-between-accounts=0s",
        "app.threads.collection.safety.delay-between-scrolls=0s"
})
@AutoConfigureMockMvc
class CollectionRunApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollectionRunRepository collectionRunRepository;

    @MockBean
    private ThreadsCollector threadsCollector;

    @BeforeEach
    void setUp() {
        collectionRunRepository.deleteAll();
    }

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

    @Test
    void deletesCollectionRun() throws Exception {
        CollectionRun run = collectionRunRepository.save(new CollectionRun(
                CollectionRunStatus.SUCCEEDED,
                1,
                1,
                0,
                1,
                LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 1, 10, 1),
                "Completed"
        ));

        mockMvc.perform(delete("/api/collection-runs/{id}", run.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/collection-runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void runsManualThreadsCollectionAndReturnsCounts() throws Exception {
        when(threadsCollector.collect(any())).thenAnswer(invocation -> {
            var request = invocation.getArgument(0, com.aifomo.dashboard.collector.threads.ThreadsCollectionRequest.class);
            return new ThreadsCollectionResult(request.source(), List.of(new ThreadsCollectedPost(
                    "https://www.threads.net/@example/post/1",
                    "New Threads AI update",
                    LocalDateTime.of(2026, 5, 5, 11, 0),
                    LocalDateTime.of(2026, 5, 5, 12, 0)
            )), List.of());
        });

        mockMvc.perform(post("/api/collection-runs/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountUrls": ["https://www.threads.net/@example"],
                                  "maxPostsPerAccount": 20,
                                  "maxScrollCount": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isNumber())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.collectedCount").value(1))
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.duplicateCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.failureReason").doesNotExist());
    }

    @Test
    void runsRecentThreadsCollectionForEnabledSources() throws Exception {
        when(threadsCollector.collect(any())).thenAnswer(invocation -> {
            var request = invocation.getArgument(0, com.aifomo.dashboard.collector.threads.ThreadsCollectionRequest.class);
            return new ThreadsCollectionResult(request.source(), List.of(new ThreadsCollectedPost(
                    request.source().getUrl() + "/post/1",
                    "New Threads AI update",
                    LocalDateTime.of(2026, 5, 5, 11, 0),
                    LocalDateTime.of(2026, 5, 5, 12, 0)
            )), List.of());
        });

        mockMvc.perform(post("/api/collection-runs/threads/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isNumber())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.collectedCount").exists())
                .andExpect(jsonPath("$.createdCount").exists())
                .andExpect(jsonPath("$.duplicateCount").exists())
                .andExpect(jsonPath("$.safetyMessage").value(org.hamcrest.Matchers.containsString("최근 3일 필터")));
    }

    @Test
    void rejectsManualThreadsCollectionWhenRunIsAlreadyRunning() throws Exception {
        collectionRunRepository.save(new CollectionRun(
                CollectionRunStatus.RUNNING,
                1,
                0,
                0,
                0,
                LocalDateTime.of(2026, 5, 5, 12, 0),
                null,
                "Running"
        ));

        mockMvc.perform(post("/api/collection-runs/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountUrls": ["https://www.threads.net/@example"],
                                  "maxPostsPerAccount": 20,
                                  "maxScrollCount": 5
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Collection run already running"));

        verifyNoInteractions(threadsCollector);
    }

    @Test
    void recordsFailureReasonWhenManualThreadsCollectionFails() throws Exception {
        when(threadsCollector.collect(any())).thenAnswer(invocation -> {
            var request = invocation.getArgument(0, com.aifomo.dashboard.collector.threads.ThreadsCollectionRequest.class);
            return ThreadsCollectionResult.failure(
                    request.source(),
                    ThreadsCollectionStatus.LOGIN_REQUIRED,
                    "Threads login required"
            );
        });

        mockMvc.perform(post("/api/collection-runs/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountUrls": ["https://www.threads.net/@example"],
                                  "maxPostsPerAccount": 20,
                                  "maxScrollCount": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.collectedCount").value(0))
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.duplicateCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.failureReason").value(org.hamcrest.Matchers.containsString("Threads login required")));
    }
}
