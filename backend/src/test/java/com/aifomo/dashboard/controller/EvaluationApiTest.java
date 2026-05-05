package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.domain.evaluation.EvaluatorType;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.repository.EvaluationRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.service.RuleBasedEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aifomo-evaluation-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EvaluationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InfoItemRepository infoItemRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Test
    void recalculatesInfoItemThroughEvaluationPipeline() throws Exception {
        InfoItem infoItem = infoItemRepository.findAll().stream()
                .filter(item -> item.getTitle().contains("Codex workflow"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/api/evaluations/info-items/{id}/recalculate", infoItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionStatus").value("APPLY"))
                .andExpect(jsonPath("$.latestEvaluation.evaluatorType").value("RULE_BASED_STUB"))
                .andExpect(jsonPath("$.latestEvaluation.evaluatorVersion").value(RuleBasedEvaluator.EVALUATOR_VERSION))
                .andExpect(jsonPath("$.latestEvaluation.reason").isNotEmpty());

        assertThat(evaluationRepository.findAll().stream()
                .filter(evaluation -> evaluation.getInfoItem().getId().equals(infoItem.getId()))
                .filter(evaluation -> evaluation.getEvaluatorType() == EvaluatorType.RULE_BASED_STUB)
                .count())
                .isEqualTo(1);
    }

    @Test
    void recalculatesUnreviewedItemsThroughEvaluationPipeline() throws Exception {
        long unreviewedCount = infoItemRepository
                .findByIsDeletedFalseAndManualOverrideFalseAndDecisionStatusOrderByCollectedAtDesc(DecisionStatus.UNREVIEWED)
                .size();

        mockMvc.perform(post("/api/evaluations/unreviewed/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize((int) unreviewedCount)))
                .andExpect(jsonPath("$[0].latestEvaluation.evaluatorType").value("RULE_BASED_STUB"));

        assertThat(evaluationRepository.findAll().stream()
                .filter(evaluation -> evaluation.getEvaluatorType() == EvaluatorType.RULE_BASED_STUB)
                .count())
                .isEqualTo(unreviewedCount);
    }

    @Test
    void manualDecisionStatusUpdateCreatesManualEvaluation() throws Exception {
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
                .andExpect(jsonPath("$.latestEvaluation.decisionStatus").value("APPLY"))
                .andExpect(jsonPath("$.latestEvaluation.evaluatorType").value("MANUAL"))
                .andExpect(jsonPath("$.latestEvaluation.confidence").value(1.0));

        assertThat(evaluationRepository.findAll().stream()
                .filter(evaluation -> evaluation.getInfoItem().getId().equals(infoItem.getId()))
                .filter(evaluation -> evaluation.getEvaluatorType() == EvaluatorType.MANUAL)
                .anyMatch(evaluation -> evaluation.getDecisionStatus() == DecisionStatus.APPLY))
                .isTrue();
    }
}
