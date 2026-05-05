package com.aifomo.dashboard.repository;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.InfoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InfoItemRepository extends JpaRepository<InfoItem, Long> {

    Optional<InfoItem> findByCollectedItem(CollectedItem collectedItem);

    List<InfoItem> findByIsDeletedFalseOrderByCollectedAtDesc();

    List<InfoItem> findByIsDeletedFalseAndIsHiddenFalseAndDecisionStatusNotInOrderByCollectedAtDesc(Collection<DecisionStatus> decisionStatuses);

    List<InfoItem> findByIsDeletedFalseAndManualOverrideFalseAndDecisionStatusOrderByCollectedAtDesc(DecisionStatus decisionStatus);

    long countByIsDeletedFalseAndDecisionStatus(DecisionStatus decisionStatus);

    long countByIsDeletedFalseAndIsHiddenFalseAndDecisionStatus(DecisionStatus decisionStatus);

    long countByIsDeletedFalseAndIsHiddenTrue();
}
