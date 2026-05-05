package com.aifomo.dashboard.service;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.collected.CollectedItemStatus;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.ImportanceLevel;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.source.SourceCategory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class RuleBasedEvaluator {

    public static final String EVALUATOR_VERSION = "rule-v1";

    private static final List<String> CORE_KEYWORDS = List.of(
            "codex", "hermes", "claude code", "claude", "openai", "browser automation",
            "local llm", "ai workflow", "developer automation", "agent workflow"
    );
    private static final List<String> ACTIONABLE_KEYWORDS = List.of(
            "usage", "guide", "setup", "setting", "configuration", "update", "workflow",
            "troubleshooting", "compare", "comparison", "tutorial", "how to", "patch",
            "verification", "automation", "사용법", "설정", "업데이트", "워크플로우",
            "문제 해결", "비교", "튜토리얼"
    );
    private static final List<String> HOLD_KEYWORDS = List.of(
            "model release", "model update", "released", "launch", "announced", "trend",
            "image generation", "video ai", "tool roundup", "tool introduce", "gemini",
            "research", "모델", "출시", "발표", "트렌드", "이미지", "영상", "도구 소개"
    );
    private static final List<String> IGNORE_KEYWORDS = List.of(
            "amazing", "viral", "follow for more", "prompts", "promo", "promotion",
            "mind blowing", "incredible", "감탄", "대박", "팔로우", "홍보"
    );
    private static final List<String> LOGIN_ONLY_KEYWORDS = List.of(
            "login to see more", "log in to see more", "로그인하여", "로그인 후"
    );
    private static final List<String> AI_KEYWORDS = List.of(
            "ai", "openai", "anthropic", "claude", "codex", "hermes", "llm", "gemini",
            "model", "agent", "automation", "image generation", "video ai", "threads",
            "인공지능", "모델", "자동화", "이미지", "영상"
    );

    public RuleBasedEvaluationResult evaluate(InfoItem infoItem) {
        String text = normalizedText(infoItem);
        CollectedItem collectedItem = infoItem.getCollectedItem();
        boolean hasCoreKeyword = containsAny(text, CORE_KEYWORDS);
        boolean hasAiKeyword = containsAny(text, AI_KEYWORDS);
        boolean hasActionableKeyword = containsAny(text, ACTIONABLE_KEYWORDS);
        boolean hasHoldKeyword = containsAny(text, HOLD_KEYWORDS);
        boolean hasIgnoreKeyword = containsAny(text, IGNORE_KEYWORDS);

        if (infoItem.isDuplicate()
                || infoItem.getDuplicateOfId() != null
                || collectedItem.getStatus() == CollectedItemStatus.DUPLICATE) {
            return archive("Duplicate content is hidden as an archive candidate.", 0.9, 0.2, 0.1, 0.05);
        }

        if (collectedItem.getStatus() == CollectedItemStatus.PARSE_FAILED) {
            return archive("Parsing failed, so this item needs archival triage.", 0.88, 0.1, 0.05, 0.05);
        }

        if (containsAny(text, LOGIN_ONLY_KEYWORDS)) {
            return archive("Content is only a login prompt without usable information.", 0.86, 0.1, 0.05, 0.05);
        }

        if (!hasAiKeyword) {
            return archive("Content is not related to AI or the configured AI sources.", 0.78, 0.08, 0.04, 0.08);
        }

        if (meaningfulLength(text) < 60 && !hasCoreKeyword) {
            return archive("Content is too short and has no core keyword signal.", 0.76, 0.12, 0.08, 0.12);
        }

        if (hasCoreKeyword && hasActionableKeyword) {
            return new RuleBasedEvaluationResult(
                    DecisionStatus.APPLY,
                    ImportanceLevel.HIGH,
                    "Core developer automation keyword and actionable workflow signal detected.",
                    0.84,
                    0.9,
                    0.86,
                    0.68
            );
        }

        if (isDeveloperCategory(infoItem) && hasCoreKeyword) {
            return new RuleBasedEvaluationResult(
                    DecisionStatus.APPLY,
                    ImportanceLevel.HIGH,
                    "Source category and core keyword indicate direct developer workflow relevance.",
                    0.8,
                    0.86,
                    0.78,
                    0.64
            );
        }

        if (hasHoldKeyword || infoItem.getCategory() == SourceCategory.IMAGE || infoItem.getCategory() == SourceCategory.VIDEO) {
            return new RuleBasedEvaluationResult(
                    DecisionStatus.HOLD,
                    ImportanceLevel.MEDIUM,
                    "AI trend, model, image/video, or tool update signal is relevant but not immediately actionable.",
                    0.72,
                    0.62,
                    0.46,
                    0.52
            );
        }

        if (hasIgnoreKeyword) {
            return new RuleBasedEvaluationResult(
                    DecisionStatus.IGNORE,
                    ImportanceLevel.LOW,
                    "Promotional or hype-oriented wording with low actionability detected.",
                    0.7,
                    0.26,
                    0.18,
                    0.2
            );
        }

        return new RuleBasedEvaluationResult(
                DecisionStatus.UNREVIEWED,
                ImportanceLevel.MEDIUM,
                "AI-related content remains ambiguous and needs manual review.",
                0.58,
                0.5,
                0.36,
                0.42
        );
    }

    private RuleBasedEvaluationResult archive(
            String reason,
            double confidence,
            double relevanceScore,
            double actionabilityScore,
            double noveltyScore
    ) {
        return new RuleBasedEvaluationResult(
                DecisionStatus.ARCHIVE_CANDIDATE,
                ImportanceLevel.LOW,
                reason,
                confidence,
                relevanceScore,
                actionabilityScore,
                noveltyScore
        );
    }

    private String normalizedText(InfoItem infoItem) {
        String rawContent = infoItem.getCollectedItem().getRawContent();
        return String.join(" ",
                        nullToEmpty(infoItem.getTitle()),
                        nullToEmpty(infoItem.getSummary()),
                        nullToEmpty(infoItem.getTags()),
                        nullToEmpty(rawContent),
                        infoItem.getCategory().name())
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private int meaningfulLength(String text) {
        return text.replaceAll("[\\s\\p{Punct}]", "").length();
    }

    private boolean isDeveloperCategory(InfoItem infoItem) {
        return infoItem.getCategory() == SourceCategory.CODEX
                || infoItem.getCategory() == SourceCategory.CLAUDE
                || infoItem.getCategory() == SourceCategory.HERMES;
    }
}
