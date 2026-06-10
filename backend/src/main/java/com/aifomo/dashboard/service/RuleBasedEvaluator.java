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
import java.util.regex.Pattern;

@Component
public class RuleBasedEvaluator {

    public static final String EVALUATOR_VERSION = "rule-v3";

    private static final List<String> CORE_KEYWORDS = List.of(
            "codex", "코덱스", "hermes", "claude code", "claude", "클로드", "openai",
            "browser automation", "local llm", "ai workflow", "developer automation", "agent workflow"
    );
    private static final List<String> ACTIONABLE_KEYWORDS = List.of(
            "usage", "guide", "setup", "setting", "configuration", "update", "workflow",
            "troubleshooting", "compare", "comparison", "tutorial", "how to", "patch",
            "verification", "automation", "available", "support", "integration", "oauth",
            "api", "sdk", "mcp", "사용법", "설정", "업데이트", "워크플로우",
            "문제 해결", "비교", "튜토리얼", "지원", "통합", "연동", "인증"
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
            "인공지능", "모델", "에이전트", "자동화", "이미지", "영상"
    );
    private static final List<String> DEVELOPER_KEYWORDS = List.of(
            "developer", "coding", "code", "api", "sdk", "oauth", "mcp", "agent",
            "automation", "개발자", "코딩", "에이전트", "자동화", "인증"
    );
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile(
            "\\b\\d{4}-\\d{2}-\\d{2}(?:[t ]\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d+)?)?(?:z|[+-]\\d{2}:?\\d{2})?)?\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("@[\\p{L}\\p{N}._]+");
    private static final Pattern THREAD_FRAGMENT_PATTERN = Pattern.compile("\\b\\d+\\s*/\\s*\\d+\\b");

    public RuleBasedEvaluationResult evaluate(InfoItem infoItem) {
        String text = normalizedText(infoItem);
        CollectedItem collectedItem = infoItem.getCollectedItem();
        boolean hasCoreKeyword = containsAny(text, CORE_KEYWORDS);
        boolean hasAiKeyword = containsAny(text, AI_KEYWORDS);
        boolean hasActionableKeyword = containsAny(text, ACTIONABLE_KEYWORDS);
        boolean hasHoldKeyword = containsAny(text, HOLD_KEYWORDS);
        boolean hasIgnoreKeyword = containsAny(text, IGNORE_KEYWORDS);
        boolean hasDeveloperKeyword = containsAny(text, DEVELOPER_KEYWORDS);
        boolean categoryRelevant = infoItem.getCategory() != SourceCategory.ETC;

        if (infoItem.isDuplicate()
                || infoItem.getDuplicateOfId() != null
                || collectedItem.getStatus() == CollectedItemStatus.DUPLICATE) {
            return archive("중복 콘텐츠로 판단되어 보관 후보로 분류했습니다.", 0.9, 0.2, 0.1, 0.05);
        }

        if (collectedItem.getStatus() == CollectedItemStatus.PARSE_FAILED) {
            return archive("콘텐츠 분석에 실패하여 보관 후보로 분류했습니다.", 0.88, 0.1, 0.05, 0.05);
        }

        if (containsAny(text, LOGIN_ONLY_KEYWORDS)) {
            return archive("로그인 안내만 있고 활용할 정보가 없어 보관 후보로 분류했습니다.", 0.86, 0.1, 0.05, 0.05);
        }

        if (THREAD_FRAGMENT_PATTERN.matcher(text).find()) {
            return unreviewed("여러 게시물로 이어지는 글의 일부로 보여 전체 맥락을 확인해야 합니다.");
        }

        if (!hasAiKeyword && !categoryRelevant) {
            return archive("AI 또는 설정된 관심 분야와 관련성이 낮아 보관 후보로 분류했습니다.", 0.78, 0.08, 0.04, 0.08);
        }

        if (meaningfulLength(text) < 40 && !hasCoreKeyword) {
            return archive("내용이 너무 짧고 핵심 키워드가 없어 보관 후보로 분류했습니다.", 0.76, 0.12, 0.08, 0.12);
        }

        if (meaningfulLength(text) < 40) {
            return unreviewed("핵심 AI 신호는 있지만 내용이 너무 짧아 신뢰할 수 있는 자동 판단이 어렵습니다.");
        }

        if (hasIgnoreKeyword && hasActionableKeyword) {
            return unreviewed("홍보성 표현과 실행 가능한 정보가 함께 있어 수동 검토가 필요합니다.");
        }

        if (hasIgnoreKeyword) {
            return new RuleBasedEvaluationResult(
                    DecisionStatus.IGNORE,
                    ImportanceLevel.LOW,
                    "홍보성 또는 과장 표현이 강하고 실행 가능성이 낮아 무시 대상으로 분류했습니다.",
                    0.78,
                    0.26,
                    0.18,
                    0.2
            );
        }

        if (hasActionableKeyword && (hasCoreKeyword || hasDeveloperKeyword || isDeveloperCategory(infoItem))) {
            return new RuleBasedEvaluationResult(
                    DecisionStatus.APPLY,
                    ImportanceLevel.HIGH,
                    "개발 워크플로우, 연동, 설정 또는 업데이트에 바로 적용할 수 있는 정보로 판단했습니다.",
                    0.86,
                    0.9,
                    0.86,
                    0.68
            );
        }

        if (hasAiKeyword
                || hasCoreKeyword
                || hasHoldKeyword
                || categoryRelevant) {
            return new RuleBasedEvaluationResult(
                    DecisionStatus.HOLD,
                    ImportanceLevel.MEDIUM,
                    "관련성 있는 AI 정보지만 즉시 적용할 신호가 충분하지 않아 나중에 볼 항목으로 분류했습니다.",
                    0.76,
                    0.62,
                    0.46,
                    0.52
            );
        }

        return unreviewed("확인 가능한 신호가 부족해 신뢰할 수 있는 자동 판단이 어렵습니다.");
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
        String text = String.join(" ",
                        nullToEmpty(infoItem.getTitle()),
                        nullToEmpty(infoItem.getSummary()),
                        nullToEmpty(infoItem.getTags()),
                        nullToEmpty(rawContent))
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        text = URL_PATTERN.matcher(text).replaceAll(" ");
        text = ISO_DATE_PATTERN.matcher(text).replaceAll(" ");
        text = AUTHOR_PATTERN.matcher(text).replaceAll(" ");
        text = removeSourceIdentity(text, infoItem);
        return text.replaceAll("\\s+", " ").trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(keyword -> containsKeyword(text, keyword));
    }

    private int meaningfulLength(String text) {
        return text.replaceAll("[\\s\\p{Punct}]", "").length();
    }

    private boolean isDeveloperCategory(InfoItem infoItem) {
        return infoItem.getCategory() == SourceCategory.CODEX
                || infoItem.getCategory() == SourceCategory.CLAUDE
                || infoItem.getCategory() == SourceCategory.HERMES;
    }

    private boolean containsKeyword(String text, String keyword) {
        if (keyword.contains(" ") || keyword.codePoints().anyMatch(codePoint -> codePoint > 127)) {
            return text.contains(keyword);
        }
        return Pattern.compile(
                "(?<![a-z0-9])" + Pattern.quote(keyword) + "(?![a-z0-9])",
                Pattern.CASE_INSENSITIVE
        ).matcher(text).find();
    }

    private String removeSourceIdentity(String text, InfoItem infoItem) {
        String result = text;
        if (infoItem.getSource() != null) {
            String sourceName = nullToEmpty(infoItem.getSource().getName()).toLowerCase(Locale.ROOT);
            if (!sourceName.isBlank()) {
                result = result.replace(sourceName, " ");
            }
            String sourceUrl = nullToEmpty(infoItem.getSource().getUrl()).toLowerCase(Locale.ROOT);
            int handleIndex = sourceUrl.indexOf("/@");
            if (handleIndex >= 0) {
                String handle = sourceUrl.substring(handleIndex + 2).replace("/", "");
                result = result.replace(handle, " ");
            }
        }
        return result;
    }

    private RuleBasedEvaluationResult unreviewed(String reason) {
        return new RuleBasedEvaluationResult(
                DecisionStatus.UNREVIEWED,
                ImportanceLevel.MEDIUM,
                reason,
                0.45,
                0.5,
                0.36,
                0.42
        );
    }
}
