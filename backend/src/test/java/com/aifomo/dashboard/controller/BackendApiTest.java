package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.collected.CollectedItemStatus;
import com.aifomo.dashboard.domain.evaluation.Evaluation;
import com.aifomo.dashboard.domain.evaluation.EvaluatorType;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.info.ImportanceLevel;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.EvaluationRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aifomo-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class BackendApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InfoItemRepository infoItemRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private CollectedItemRepository collectedItemRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @BeforeEach
    void setUp() {
        evaluationRepository.deleteAll();
        infoItemRepository.deleteAll();
        collectedItemRepository.deleteAll();

        Source newsSource = sourceRepository.findAllByOrderByPriorityAscIdAsc().stream()
                .filter(source -> source.getName().equals("Choi OpenAI"))
                .findFirst()
                .orElseThrow();

        createInfoItem(
                newsSource,
                "Codex workflow setup",
                "Codex workflow update shows a practical agent setup for code review and verification.",
                DecisionStatus.APPLY,
                false
        );
        createInfoItem(
                newsSource,
                "OpenAI model update for tool use",
                "OpenAI announced a new model update with better tool use and lower latency.",
                DecisionStatus.HOLD,
                false
        );
        createInfoItem(
                newsSource,
                "Viral AI video examples",
                "Amazing AI video results are going viral today, follow for more prompts.",
                DecisionStatus.IGNORE,
                true
        );
    }

    @Test
    void exposesSourceList() throws Exception {
        mockMvc.perform(get("/api/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].sourceType").value("THREADS_ACCOUNT"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].priority").value(10))
                .andExpect(jsonPath("$[7].sourceType").value("RSS_FEED"));
    }

    @Test
    void updatesSourceEnabledState() throws Exception {
        Source source = sourceRepository.findAllByOrderByPriorityAscIdAsc().getFirst();

        mockMvc.perform(patch("/api/sources/{id}/enabled", source.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(source.getId()))
                .andExpect(jsonPath("$.enabled").value(false));

        Source updated = sourceRepository.findById(source.getId()).orElseThrow();
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    void exposesInfoItemListDetailAndLatestEvaluation() throws Exception {
        InfoItem infoItem = infoItemRepository.findAll().getFirst();

        mockMvc.perform(get("/api/info-items").param("includeHidden", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].latestEvaluation").exists());

        mockMvc.perform(get("/api/info-items/{id}", infoItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(infoItem.getId()))
                .andExpect(jsonPath("$.collectedItemId").value(infoItem.getCollectedItem().getId()));
    }

    @Test
    void manuallyUpdatesDecisionStatusAndManualOverride() throws Exception {
        InfoItem infoItem = infoItemRepository.findAll().stream()
                .filter(item -> item.getDecisionStatus() != DecisionStatus.APPLY)
                .findFirst()
                .orElseThrow();

        mockMvc.perform(patch("/api/info-items/{id}/decision-status", infoItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionStatus\":\"APPLY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionStatus").value("APPLY"))
                .andExpect(jsonPath("$.manualOverride").value(true))
                .andExpect(jsonPath("$.hidden").value(false))
                .andExpect(jsonPath("$.latestEvaluation.evaluatorType").value("MANUAL"));

        InfoItem updated = infoItemRepository.findById(infoItem.getId()).orElseThrow();
        assertThat(updated.getDecisionStatus()).isEqualTo(DecisionStatus.APPLY);
        assertThat(updated.isManualOverride()).isTrue();
        assertThat(updated.isHidden()).isFalse();
    }

    @Test
    void archivesAndRestoresWithoutDeleting() throws Exception {
        InfoItem infoItem = infoItemRepository.findAll().getFirst();

        mockMvc.perform(patch("/api/info-items/{id}/archive", infoItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionStatus").value("ARCHIVE_CANDIDATE"))
                .andExpect(jsonPath("$.hidden").value(true))
                .andExpect(jsonPath("$.deleted").value(false))
                .andExpect(jsonPath("$.manualOverride").value(true));

        mockMvc.perform(patch("/api/info-items/{id}/restore", infoItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionStatus").value("UNREVIEWED"))
                .andExpect(jsonPath("$.hidden").value(false))
                .andExpect(jsonPath("$.deleted").value(false))
                .andExpect(jsonPath("$.manualOverride").value(true));
    }

    @Test
    void exposesDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.visibleCount", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.applyCount", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.holdCount", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.unreviewedCount", greaterThanOrEqualTo(0)));
    }

    @Test
    void allowsLocalFrontendCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/info-items")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private void createInfoItem(
            Source source,
            String title,
            String summary,
            DecisionStatus decisionStatus,
            boolean hidden
    ) {
        CollectedItem collectedItem = collectedItemRepository.save(new CollectedItem(
                source,
                source.getUrl() + "/post/" + title.toLowerCase().replace(" ", "-"),
                summary,
                source.getId() + "-" + title.toLowerCase().replace(" ", "-"),
                CollectedItemStatus.COLLECTED,
                java.time.LocalDateTime.of(2026, 5, 5, 12, 0)
        ));
        InfoItem infoItem = infoItemRepository.save(new InfoItem(
                source,
                collectedItem,
                title,
                summary,
                collectedItem.getRawUrl(),
                source.getCategory(),
                "[]",
                ImportanceLevel.MEDIUM,
                decisionStatus,
                false,
                hidden,
                false,
                null,
                false,
                java.time.LocalDateTime.of(2026, 5, 5, 11, 0),
                java.time.LocalDateTime.of(2026, 5, 5, 12, 0)
        ));
        evaluationRepository.save(new Evaluation(
                infoItem,
                decisionStatus,
                "Initial test evaluation",
                0.8,
                0.8,
                0.7,
                0.6,
                EvaluatorType.RULE_BASED_STUB,
                "test-v1"
        ));
    }
}
