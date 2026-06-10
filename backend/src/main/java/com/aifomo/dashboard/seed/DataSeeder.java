package com.aifomo.dashboard.seed;

import com.aifomo.dashboard.domain.evaluation.Evaluation;
import com.aifomo.dashboard.domain.evaluation.EvaluatorType;
import com.aifomo.dashboard.domain.info.InfoItem;
import com.aifomo.dashboard.domain.source.RetiredSourcePolicy;
import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceCategory;
import com.aifomo.dashboard.domain.source.SourceType;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.EvaluationRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.repository.SourceRepository;
import com.aifomo.dashboard.service.EvaluationReasonLocalizer;
import com.aifomo.dashboard.service.RuleBasedEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final SourceRepository sourceRepository;
    private final CollectedItemRepository collectedItemRepository;
    private final InfoItemRepository infoItemRepository;
    private final EvaluationRepository evaluationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedSources();
        retireRemovedSources();
        removeSeedSampleData();
        localizeEvaluationReasons();
    }

    private void seedSources() {
        List<SourceSeed> sources = List.of(
                new SourceSeed("Choi OpenAI", SourceType.THREADS_ACCOUNT, SourceCategory.NEWS, "https://www.threads.com/@choi.openai", "Threads AI news curation", true, 10),
                new SourceSeed("UncleJobs AI", SourceType.THREADS_ACCOUNT, SourceCategory.NEWS, "https://www.threads.com/@unclejobs.ai", "Threads AI news curation", true, 20),
                new SourceSeed("GPTaku AI", SourceType.THREADS_ACCOUNT, SourceCategory.CLAUDE, "https://www.threads.com/@gptaku_ai", "Claude and AI coding threads", true, 50),
                new SourceSeed("Roach Log", SourceType.THREADS_ACCOUNT, SourceCategory.HERMES, "https://www.threads.com/@roach_log", "Hermes and local AI workflow threads", true, 60),
                new SourceSeed("OpenAI Official Blog", SourceType.OFFICIAL_BLOG, SourceCategory.COMPANY_OFFICIAL, "https://openai.com/blog", "Future official source", false, 100),
                new SourceSeed("Anthropic News", SourceType.OFFICIAL_BLOG, SourceCategory.COMPANY_OFFICIAL, "https://www.anthropic.com/news", "Future official source", false, 110),
                new SourceSeed("Google AI Blog", SourceType.OFFICIAL_BLOG, SourceCategory.COMPANY_OFFICIAL, "https://ai.googleblog.com", "Future official source", false, 120),
                new SourceSeed("OpenAI News RSS", SourceType.RSS_FEED, SourceCategory.COMPANY_OFFICIAL, "https://openai.com/news/rss.xml", "Future RSS source", false, 130)
        );

        sources.forEach(this::upsertSourceSeed);
    }

    private void retireRemovedSources() {
        sourceRepository.findAll().stream()
                .filter(RetiredSourcePolicy::isRetired)
                .forEach(source -> source.setEnabled(false));
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

    private void removeSeedSampleData() {
        List<Evaluation> seededEvaluations = evaluationRepository.findByEvaluatorType(EvaluatorType.SEED_SAMPLE);
        if (seededEvaluations.isEmpty()) {
            return;
        }

        Set<Long> infoItemIds = new LinkedHashSet<>();
        Set<Long> collectedItemIds = new LinkedHashSet<>();
        for (Evaluation evaluation : seededEvaluations) {
            InfoItem infoItem = evaluation.getInfoItem();
            if (infoItem == null) {
                continue;
            }
            infoItemIds.add(infoItem.getId());
            if (infoItem.getCollectedItem() != null) {
                collectedItemIds.add(infoItem.getCollectedItem().getId());
            }
        }

        evaluationRepository.deleteAllInBatch(seededEvaluations);
        if (!infoItemIds.isEmpty()) {
            infoItemRepository.deleteAllByIdInBatch(infoItemIds);
        }
        if (!collectedItemIds.isEmpty()) {
            collectedItemRepository.deleteAllByIdInBatch(collectedItemIds);
        }
    }

    private void localizeEvaluationReasons() {
        evaluationRepository.findAll().forEach(evaluation -> {
            evaluation.setReason(EvaluationReasonLocalizer.localize(evaluation.getReason()));
            if (evaluation.getEvaluatorType() == EvaluatorType.RULE_BASED_STUB) {
                evaluation.setEvaluatorVersion(RuleBasedEvaluator.EVALUATOR_VERSION);
            }
        });
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

}
