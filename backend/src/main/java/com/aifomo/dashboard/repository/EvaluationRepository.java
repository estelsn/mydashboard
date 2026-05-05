package com.aifomo.dashboard.repository;

import com.aifomo.dashboard.domain.evaluation.Evaluation;
import com.aifomo.dashboard.domain.evaluation.EvaluatorType;
import com.aifomo.dashboard.domain.info.InfoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    boolean existsByInfoItemAndEvaluatorType(InfoItem infoItem, EvaluatorType evaluatorType);
}
