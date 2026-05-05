package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.repository.SourceRepository;
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

    @Test
    void exposesSourceList() throws Exception {
        mockMvc.perform(get("/api/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(13)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].sourceType").value("THREADS_ACCOUNT"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].priority").value(10))
                .andExpect(jsonPath("$[12].sourceType").value("RSS_FEED"));
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
                .andExpect(jsonPath("$", hasSize(12)))
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
                .andExpect(jsonPath("$.hidden").value(false));

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
                .andExpect(jsonPath("$.totalCount").value(12))
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
}
