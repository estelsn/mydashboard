package com.aifomo.dashboard.seed;

import com.aifomo.dashboard.domain.source.SourceType;
import com.aifomo.dashboard.repository.CollectedItemRepository;
import com.aifomo.dashboard.repository.EvaluationRepository;
import com.aifomo.dashboard.repository.InfoItemRepository;
import com.aifomo.dashboard.repository.SourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aifomo-seed-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DataSeederTest {

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private CollectedItemRepository collectedItemRepository;

    @Autowired
    private InfoItemRepository infoItemRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Test
    void seedsOnlySourcesAndRemovesSeedSamplesWithoutDuplicates() {
        dataSeeder.run();
        dataSeeder.run();

        assertThat(sourceRepository.count()).isEqualTo(13);
        assertThat(collectedItemRepository.count()).isZero();
        assertThat(infoItemRepository.count()).isZero();
        assertThat(evaluationRepository.count()).isZero();
        assertThat(sourceRepository.findAll())
                .anySatisfy(source -> {
                    assertThat(source.getSourceType()).isEqualTo(SourceType.THREADS_ACCOUNT);
                    assertThat(source.isEnabled()).isTrue();
                    assertThat(source.getPriority()).isPositive();
                })
                .anySatisfy(source -> assertThat(source.getSourceType()).isEqualTo(SourceType.OFFICIAL_BLOG))
                .anySatisfy(source -> assertThat(source.getSourceType()).isEqualTo(SourceType.RSS_FEED));
    }
}
