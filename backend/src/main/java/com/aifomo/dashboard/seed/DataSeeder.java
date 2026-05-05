package com.aifomo.dashboard.seed;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.collected.CollectedItemStatus;
import com.aifomo.dashboard.domain.evaluation.Evaluation;
import com.aifomo.dashboard.domain.evaluation.EvaluatorType;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.ImportanceLevel;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.EvaluationRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.repository.SourceRepository;
import com.aifomo.dashboard.util.ContentHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String SEED_EVALUATOR_VERSION = "seed-v1";

    private final SourceRepository sourceRepository;
    private final CollectedItemRepository collectedItemRepository;
    private final InfoItemRepository infoItemRepository;
    private final EvaluationRepository evaluationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedSources();
        seedItems();
    }

    private void seedSources() {
        List<SourceSeed> sources = List.of(
                new SourceSeed("Choi OpenAI", SourceType.THREADS_ACCOUNT, SourceCategory.NEWS, "https://www.threads.com/@choi.openai", "Threads AI news curation", true, 10),
                new SourceSeed("UncleJobs AI", SourceType.THREADS_ACCOUNT, SourceCategory.NEWS, "https://www.threads.com/@unclejobs.ai", "Threads AI news curation", true, 20),
                new SourceSeed("Appcast", SourceType.THREADS_ACCOUNT, SourceCategory.CODEX, "https://www.threads.com/@appcast", "Codex and developer workflow threads", true, 30),
                new SourceSeed("Ethan CL", SourceType.THREADS_ACCOUNT, SourceCategory.CODEX, "https://www.threads.com/@ethancl", "Codex and automation threads", true, 40),
                new SourceSeed("GPTaku AI", SourceType.THREADS_ACCOUNT, SourceCategory.CLAUDE, "https://www.threads.com/@gptaku_ai", "Claude and AI coding threads", true, 50),
                new SourceSeed("Roach Log", SourceType.THREADS_ACCOUNT, SourceCategory.HERMES, "https://www.threads.com/@roach_log", "Hermes and local AI workflow threads", true, 60),
                new SourceSeed("Specal1849", SourceType.THREADS_ACCOUNT, SourceCategory.IMAGE, "https://www.threads.com/@specal1849", "Image AI threads", true, 70),
                new SourceSeed("Xazinga", SourceType.THREADS_ACCOUNT, SourceCategory.VIDEO, "https://www.threads.com/@xazinga", "Video AI threads", true, 80),
                new SourceSeed("Apple Tea 94", SourceType.THREADS_ACCOUNT, SourceCategory.VIDEO, "https://www.threads.com/@apple_tea_94", "Video AI threads", true, 90),
                new SourceSeed("OpenAI Official Blog", SourceType.OFFICIAL_BLOG, SourceCategory.COMPANY_OFFICIAL, "https://openai.com/blog", "Future official source", false, 100),
                new SourceSeed("Anthropic News", SourceType.OFFICIAL_BLOG, SourceCategory.COMPANY_OFFICIAL, "https://www.anthropic.com/news", "Future official source", false, 110),
                new SourceSeed("Google AI Blog", SourceType.OFFICIAL_BLOG, SourceCategory.COMPANY_OFFICIAL, "https://ai.googleblog.com", "Future official source", false, 120),
                new SourceSeed("OpenAI News RSS", SourceType.RSS_FEED, SourceCategory.COMPANY_OFFICIAL, "https://openai.com/news/rss.xml", "Future RSS source", false, 130)
        );

        sources.forEach(this::upsertSourceSeed);
    }

    private void upsertSourceSeed(SourceSeed seed) {
        sourceRepository.findByUrl(seed.url())
                .ifPresentOrElse(source -> {
                    source.setName(seed.name());
                    source.setSourceType(seed.sourceType());
                    source.setCategory(seed.category());
                    source.setDescription(seed.description());
                    source.setEnabled(seed.enabled());
                    source.setPriority(seed.priority());
                }, () -> sourceRepository.save(new Source(
                        seed.name(),
                        seed.sourceType(),
                        seed.category(),
                        seed.url(),
                        seed.description(),
                        seed.enabled(),
                        seed.priority()
                )));
    }

    private void seedItems() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 1, 9, 0);
        List<ItemSeed> items = List.of(
                new ItemSeed("https://www.threads.com/@appcast", "https://www.threads.com/@appcast/post/seed-001", "Codex workflow update shows a practical agent setup for code review, patching, and verification in a local project.", "Codex workflow setup", "Agent workflow for code review and local patch verification.", "[\"codex\",\"workflow\",\"automation\"]", ImportanceLevel.HIGH, DecisionStatus.APPLY, 0.92, 0.94, 0.88, 0.76, "Directly relevant to developer automation work."),
                new ItemSeed("https://www.threads.com/@ethancl", "https://www.threads.com/@ethancl/post/seed-002", "A compact guide compares Codex task planning with manual terminal workflows and shows when to keep changes small.", "Codex task planning comparison", "Comparison of Codex planning and manual terminal workflows.", "[\"codex\",\"planning\",\"developer-tools\"]", ImportanceLevel.HIGH, DecisionStatus.APPLY, 0.89, 0.9, 0.82, 0.72, "High relevance and practical workflow value."),
                new ItemSeed("https://www.threads.com/@gptaku_ai", "https://www.threads.com/@gptaku_ai/post/seed-003", "Claude Code usage notes cover project memory, command approval, and keeping implementation scoped to the requested step.", "Claude Code scoped implementation notes", "Notes about scoped coding agent behavior and approvals.", "[\"claude-code\",\"agent\",\"workflow\"]", ImportanceLevel.HIGH, DecisionStatus.APPLY, 0.86, 0.88, 0.8, 0.7, "Useful for coding agent operating practices."),
                new ItemSeed("https://www.threads.com/@roach_log", "https://www.threads.com/@roach_log/post/seed-004", "Hermes local LLM routing can reduce latency for repetitive summarization, but setup details need later review.", "Hermes local LLM routing", "Local LLM routing idea for repeated summarization work.", "[\"hermes\",\"local-llm\",\"summarization\"]", ImportanceLevel.MEDIUM, DecisionStatus.HOLD, 0.78, 0.72, 0.58, 0.74, "Potentially useful but not immediately actionable."),
                new ItemSeed("https://www.threads.com/@choi.openai", "https://www.threads.com/@choi.openai/post/seed-005", "OpenAI announced a new model update with better tool use and lower latency for coding assistance scenarios.", "OpenAI model update for tool use", "Model update may affect coding assistant workflows.", "[\"openai\",\"model-update\",\"tool-use\"]", ImportanceLevel.MEDIUM, DecisionStatus.HOLD, 0.8, 0.76, 0.54, 0.82, "Relevant trend, official confirmation should come later."),
                new ItemSeed("https://www.threads.com/@unclejobs.ai", "https://www.threads.com/@unclejobs.ai/post/seed-006", "A roundup lists three AI developer tools released this week, including one browser automation helper.", "AI developer tools weekly roundup", "Weekly roundup with possible browser automation lead.", "[\"ai-tools\",\"browser-automation\",\"roundup\"]", ImportanceLevel.MEDIUM, DecisionStatus.HOLD, 0.72, 0.7, 0.48, 0.66, "Some relevance but needs manual triage."),
                new ItemSeed("https://www.threads.com/@specal1849", "https://www.threads.com/@specal1849/post/seed-007", "Image generation workflow update improves reference consistency for product mockups and thumbnails.", "Image AI reference consistency update", "Image generation update for consistent reference outputs.", "[\"image-ai\",\"mockups\",\"creative\"]", ImportanceLevel.LOW, DecisionStatus.HOLD, 0.58, 0.54, 0.34, 0.62, "Useful later, lower priority for current workflow."),
                new ItemSeed("https://www.threads.com/@xazinga", "https://www.threads.com/@xazinga/post/seed-008", "Video AI tool adds timeline editing and auto captioning, mainly aimed at creators.", "Video AI timeline editing", "Creator-focused video AI update.", "[\"video-ai\",\"captions\",\"creator-tools\"]", ImportanceLevel.LOW, DecisionStatus.IGNORE, 0.36, 0.32, 0.18, 0.56, "Low current relevance to dashboard implementation."),
                new ItemSeed("https://www.threads.com/@apple_tea_94", "https://www.threads.com/@apple_tea_94/post/seed-009", "Amazing AI video results are going viral today, follow for more examples and prompts.", "Viral AI video examples", "Mostly promotional viral video post.", "[\"video-ai\",\"promo\"]", ImportanceLevel.LOW, DecisionStatus.IGNORE, 0.22, 0.2, 0.12, 0.5, "Promotional tone and low actionability."),
                new ItemSeed("https://openai.com/blog", "https://openai.com/blog/seed-010", "Official OpenAI blog placeholder source for future verification of model releases and product announcements.", "OpenAI official source placeholder", "Future official verification source seed.", "[\"official\",\"openai\",\"future-source\"]", ImportanceLevel.MEDIUM, DecisionStatus.UNREVIEWED, 0.64, 0.5, 0.4, 0.8, "Official source is registered but not collected automatically in Step 1."),
                new ItemSeed("https://www.anthropic.com/news", "https://www.anthropic.com/news/seed-011", "Anthropic news placeholder source for future verification of Claude releases and safety announcements.", "Anthropic official source placeholder", "Future official verification source seed.", "[\"official\",\"anthropic\",\"future-source\"]", ImportanceLevel.MEDIUM, DecisionStatus.UNREVIEWED, 0.62, 0.48, 0.4, 0.8, "Official source is registered but not collected automatically in Step 1."),
                new ItemSeed("https://ai.googleblog.com", "https://ai.googleblog.com/seed-012", "Google AI Blog placeholder source for future verification of Gemini and research announcements.", "Google AI official source placeholder", "Future official verification source seed.", "[\"official\",\"google-ai\",\"future-source\"]", ImportanceLevel.MEDIUM, DecisionStatus.UNREVIEWED, 0.6, 0.46, 0.4, 0.8, "Official source is registered but not collected automatically in Step 1.")
        );

        for (int index = 0; index < items.size(); index++) {
            ItemSeed seed = items.get(index);
            Source source = sourceRepository.findByUrl(seed.sourceUrl())
                    .orElseThrow(() -> new IllegalStateException("Missing source seed: " + seed.sourceUrl()));
            LocalDateTime collectedAt = baseTime.plusHours(index);
            String contentHash = ContentHashUtil.sha256Normalized(seed.rawContent());

            CollectedItem collectedItem = collectedItemRepository.findByContentHash(contentHash)
                    .orElseGet(() -> collectedItemRepository.save(new CollectedItem(
                            source,
                            seed.rawUrl(),
                            seed.rawContent(),
                            contentHash,
                            CollectedItemStatus.COLLECTED,
                            collectedAt
                    )));

            InfoItem infoItem = infoItemRepository.findByCollectedItem(collectedItem)
                    .orElseGet(() -> infoItemRepository.save(new InfoItem(
                            source,
                            collectedItem,
                            seed.title(),
                            seed.summary(),
                            seed.rawUrl(),
                            source.getCategory(),
                            seed.tags(),
                            seed.importanceLevel(),
                            seed.decisionStatus(),
                            false,
                            seed.decisionStatus() == DecisionStatus.IGNORE || seed.decisionStatus() == DecisionStatus.ARCHIVE_CANDIDATE,
                            false,
                            null,
                            false,
                            collectedAt.minusMinutes(30),
                            collectedAt
                    )));

            if (!evaluationRepository.existsByInfoItemAndEvaluatorType(infoItem, EvaluatorType.SEED_SAMPLE)) {
                evaluationRepository.save(new Evaluation(
                        infoItem,
                        seed.decisionStatus(),
                        seed.reason(),
                        seed.confidence(),
                        seed.relevanceScore(),
                        seed.actionabilityScore(),
                        seed.noveltyScore(),
                        EvaluatorType.SEED_SAMPLE,
                        SEED_EVALUATOR_VERSION
                ));
            }
        }
    }

    private record SourceSeed(
            String name,
            SourceType sourceType,
            SourceCategory category,
            String url,
            String description,
            boolean enabled,
            int priority
    ) {
    }

    private record ItemSeed(
            String sourceUrl,
            String rawUrl,
            String rawContent,
            String title,
            String summary,
            String tags,
            ImportanceLevel importanceLevel,
            DecisionStatus decisionStatus,
            double confidence,
            double relevanceScore,
            double actionabilityScore,
            double noveltyScore,
            String reason
    ) {
    }
}
