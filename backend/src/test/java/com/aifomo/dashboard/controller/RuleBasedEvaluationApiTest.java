package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.collected.CollectedItemStatus;
import com.aifomo.dashboard.domain.evaluation.EvaluatorType;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.info.ImportanceLevel;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.EvaluationRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.repository.SourceRepository;
import com.aifomo.dashboard.service.RuleBasedEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aifomo-rule-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RuleBasedEvaluationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InfoItemRepository infoItemRepository;

    @Autowired
    private CollectedItemRepository collectedItemRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @BeforeEach
    void setUp() {
        evaluationRepository.deleteAll();
        infoItemRepository.deleteAll();
        collectedItemRepository.deleteAll();

        Source codexSource = sourceRepository.findAllByOrderByPriorityAscIdAsc().stream()
                .filter(source -> source.getName().equals("Appcast"))
                .findFirst()
                .orElseThrow();
        Source newsSource = sourceRepository.findAllByOrderByPriorityAscIdAsc().stream()
                .filter(source -> source.getName().equals("Choi OpenAI"))
                .findFirst()
                .orElseThrow();

        createInfoItem(
                codexSource,
                "Codex workflow setup",
                "Codex workflow update shows a practical agent setup for code review and local patch verification.",
                DecisionStatus.UNREVIEWED
        );
        createInfoItem(
                newsSource,
                "OpenAI model update for tool use",
                "OpenAI announced a new model update with better tool use and lower latency for coding assistance.",
                DecisionStatus.UNREVIEWED
        );
    }

    @Test
    void evaluatesIndividualInfoItemWithRuleBasedMetadata() throws Exception {
        InfoItem infoItem = infoItemRepository.findAll().stream()
                .filter(item -> item.getTitle().contains("Codex workflow"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/api/rule-based-evaluations/info-items/{id}", infoItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionStatus").value("APPLY"))
                .andExpect(jsonPath("$.importanceLevel").value("HIGH"))
                .andExpect(jsonPath("$.latestEvaluation.evaluatorType").value("RULE_BASED_STUB"))
                .andExpect(jsonPath("$.latestEvaluation.evaluatorVersion").value(RuleBasedEvaluator.EVALUATOR_VERSION));

        assertThat(evaluationRepository.findAll().stream()
                .filter(evaluation -> evaluation.getInfoItem().getId().equals(infoItem.getId()))
                .filter(evaluation -> evaluation.getEvaluatorType() == EvaluatorType.RULE_BASED_STUB)
                .anyMatch(evaluation -> RuleBasedEvaluator.EVALUATOR_VERSION.equals(evaluation.getEvaluatorVersion())))
                .isTrue();
    }

    @Test
    void evaluatesUnreviewedItemsInBatch() throws Exception {
        long unreviewedCount = infoItemRepository
                .findByIsDeletedFalseAndManualOverrideFalseAndDecisionStatusOrderByCollectedAtDesc(DecisionStatus.UNREVIEWED)
                .size();

        mockMvc.perform(post("/api/rule-based-evaluations/unreviewed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize((int) unreviewedCount)))
                .andExpect(jsonPath("$[0].latestEvaluation.evaluatorType").value("RULE_BASED_STUB"))
                .andExpect(jsonPath("$[0].latestEvaluation.evaluatorVersion").value(RuleBasedEvaluator.EVALUATOR_VERSION));

        assertThat(evaluationRepository.findAll().stream()
                .filter(evaluation -> evaluation.getEvaluatorType() == EvaluatorType.RULE_BASED_STUB)
                .count())
                .isGreaterThanOrEqualTo(unreviewedCount);
    }

    @Test
    void doesNotOverwriteManualOverrideDecisionStatus() throws Exception {
        InfoItem infoItem = infoItemRepository.findAll().stream()
                .filter(item -> item.getTitle().contains("Codex workflow"))
                .findFirst()
                .orElseThrow();
        infoItem.setDecisionStatus(DecisionStatus.IGNORE);
        infoItem.setHidden(true);
        infoItem.setManualOverride(true);
        infoItemRepository.saveAndFlush(infoItem);

        mockMvc.perform(post("/api/rule-based-evaluations/info-items/{id}", infoItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionStatus").value("IGNORE"))
                .andExpect(jsonPath("$.manualOverride").value(true))
                .andExpect(jsonPath("$.hidden").value(true))
                .andExpect(jsonPath("$.latestEvaluation.decisionStatus").value("APPLY"))
                .andExpect(jsonPath("$.latestEvaluation.evaluatorType").value("RULE_BASED_STUB"));

        InfoItem updated = infoItemRepository.findById(infoItem.getId()).orElseThrow();
        assertThat(updated.getDecisionStatus()).isEqualTo(DecisionStatus.IGNORE);
        assertThat(updated.isManualOverride()).isTrue();
        assertThat(updated.isHidden()).isTrue();
    }

    private void createInfoItem(Source source, String title, String summary, DecisionStatus decisionStatus) {
        String slug = title.toLowerCase().replace(" ", "-");
        CollectedItem collectedItem = collectedItemRepository.save(new CollectedItem(
                source,
                source.getUrl() + "/post/" + slug,
                summary,
                source.getId() + "-" + slug,
                CollectedItemStatus.COLLECTED,
                java.time.LocalDateTime.of(2026, 5, 5, 12, 0)
        ));
        infoItemRepository.save(new InfoItem(
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
                false,
                false,
                null,
                false,
                java.time.LocalDateTime.of(2026, 5, 5, 11, 0),
                java.time.LocalDateTime.of(2026, 5, 5, 12, 0)
        ));
    }
}
