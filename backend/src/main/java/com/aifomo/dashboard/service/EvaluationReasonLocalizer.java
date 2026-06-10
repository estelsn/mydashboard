package com.aifomo.dashboard.service;

import java.util.Map;

public final class EvaluationReasonLocalizer {

    private static final Map<String, String> LEGACY_REASONS = Map.ofEntries(
            Map.entry("Duplicate content is hidden as an archive candidate.", "중복 콘텐츠로 판단되어 보관 후보로 분류했습니다."),
            Map.entry("Parsing failed, so this item needs archival triage.", "콘텐츠 분석에 실패하여 보관 후보로 분류했습니다."),
            Map.entry("Content is only a login prompt without usable information.", "로그인 안내만 있고 활용할 정보가 없어 보관 후보로 분류했습니다."),
            Map.entry("This appears to be an incomplete multi-post thread and needs context.", "여러 게시물로 이어지는 글의 일부로 보여 전체 맥락을 확인해야 합니다."),
            Map.entry("Content is not related to AI or the configured AI sources.", "AI 또는 설정된 관심 분야와 관련성이 낮아 보관 후보로 분류했습니다."),
            Map.entry("Content is too short and has no core keyword signal.", "내용이 너무 짧고 핵심 키워드가 없어 보관 후보로 분류했습니다."),
            Map.entry("Core AI signal exists, but the content is too short for a reliable decision.", "핵심 AI 신호는 있지만 내용이 너무 짧아 신뢰할 수 있는 자동 판단이 어렵습니다."),
            Map.entry("Promotional and actionable signals conflict, so manual review is required.", "홍보성 표현과 실행 가능한 정보가 함께 있어 수동 검토가 필요합니다."),
            Map.entry("Promotional or hype-oriented wording with low actionability detected.", "홍보성 또는 과장 표현이 강하고 실행 가능성이 낮아 무시 대상으로 분류했습니다."),
            Map.entry("Actionable developer workflow, integration, setup, or update signal detected.", "개발 워크플로우, 연동, 설정 또는 업데이트에 바로 적용할 수 있는 정보로 판단했습니다."),
            Map.entry("Relevant AI information was detected without a strong immediate action signal.", "관련성 있는 AI 정보지만 즉시 적용할 신호가 충분하지 않아 나중에 볼 항목으로 분류했습니다."),
            Map.entry("The available signals are insufficient for a reliable automatic decision.", "확인 가능한 신호가 부족해 신뢰할 수 있는 자동 판단이 어렵습니다."),
            Map.entry("Manual decision status update.", "사용자가 분류 상태를 직접 변경했습니다."),
            Map.entry("LLM evaluation stub recorded without external API calls.", "외부 API 호출 없이 LLM 평가 준비 상태만 기록했습니다.")
    );

    private EvaluationReasonLocalizer() {
    }

    public static String localize(String reason) {
        return reason == null ? null : LEGACY_REASONS.getOrDefault(reason, reason);
    }
}
