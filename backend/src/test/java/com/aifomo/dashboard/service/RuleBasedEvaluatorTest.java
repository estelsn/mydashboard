package com.aifomo.dashboard.service;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.collected.CollectedItemStatus;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.ImportanceLevel;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedEvaluatorTest {

    private final RuleBasedEvaluator evaluator = new RuleBasedEvaluator();

    @Test
    void classifiesActionableDeveloperUpdateAsApply() {
        RuleBasedEvaluationResult result = evaluator.evaluate(infoItem(
                SourceCategory.CODEX,
                "Codex OAuth integration setup guide for developer automation workflows."
        ));

        assertThat(result.decisionStatus()).isEqualTo(DecisionStatus.APPLY);
        assertThat(result.importanceLevel()).isEqualTo(ImportanceLevel.HIGH);
        assertThat(result.reason()).contains("바로 적용");
    }

    @Test
    void classifiesGeneralAiNewsAsHoldInsteadOfUnreviewed() {
        RuleBasedEvaluationResult result = evaluator.evaluate(infoItem(
                SourceCategory.NEWS,
                "NVIDIA announced a new AI research platform for humanoid robotics."
        ));

        assertThat(result.decisionStatus()).isEqualTo(DecisionStatus.HOLD);
        assertThat(result.reason()).contains("나중에 볼 항목");
    }

    @Test
    void classifiesPromotionalContentBeforeTrendSignals() {
        RuleBasedEvaluationResult result = evaluator.evaluate(infoItem(
                SourceCategory.VIDEO,
                "Amazing viral video AI results. Follow for more prompts and promotion."
        ));

        assertThat(result.decisionStatus()).isEqualTo(DecisionStatus.IGNORE);
    }

    @Test
    void leavesIncompleteThreadFragmentForReview() {
        RuleBasedEvaluationResult result = evaluator.evaluate(infoItem(
                SourceCategory.NEWS,
                "Enterprise AI infrastructure analysis continues in the next post. 1 / 2"
        ));

        assertThat(result.decisionStatus()).isEqualTo(DecisionStatus.UNREVIEWED);
    }

    @Test
    void doesNotTreatTaipeiAsAiKeyword() {
        RuleBasedEvaluationResult result = evaluator.evaluate(infoItem(
                SourceCategory.ETC,
                "Taipei keynote replay about general semiconductor manufacturing operations."
        ));

        assertThat(result.decisionStatus()).isEqualTo(DecisionStatus.ARCHIVE_CANDIDATE);
    }

    @Test
    void removesAuthorHandleFromEvaluationSignals() {
        RuleBasedEvaluationResult result = evaluator.evaluate(infoItem(
                SourceCategory.ETC,
                "@choi.openai Taipei keynote replay about semiconductor manufacturing."
        ));

        assertThat(result.decisionStatus()).isEqualTo(DecisionStatus.ARCHIVE_CANDIDATE);
    }

    private InfoItem infoItem(SourceCategory category, String content) {
        Source source = new Source(
                "Choi OpenAI",
                SourceType.THREADS_ACCOUNT,
                category,
                "https://www.threads.com/@choi.openai",
                "test",
                true
        );
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0);
        CollectedItem collectedItem = new CollectedItem(
                source,
                source.getUrl() + "/post/test",
                content,
                Integer.toHexString(content.hashCode()),
                CollectedItemStatus.COLLECTED,
                now
        );
        return new InfoItem(
                source,
                collectedItem,
                content,
                content,
                collectedItem.getRawUrl(),
                category,
                "[]",
                ImportanceLevel.MEDIUM,
                DecisionStatus.UNREVIEWED,
                false,
                false,
                false,
                null,
                false,
                now,
                now
        );
    }
}
