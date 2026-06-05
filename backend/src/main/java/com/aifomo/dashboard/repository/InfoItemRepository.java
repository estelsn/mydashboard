package com.aifomo.dashboard.repository;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.info.DecisionStatus;
import com.aifomo.dashboard.domain.info.InfoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InfoItemRepository extends JpaRepository<InfoItem, Long> {

    Optional<InfoItem> findByCollectedItem(CollectedItem collectedItem);

    @Query("""
            select infoItem
            from InfoItem infoItem
            where infoItem.isDeleted = false
            order by
                case when infoItem.publishedAt is null then 1 else 0 end asc,
                infoItem.publishedAt desc,
                infoItem.collectedAt desc,
                infoItem.id desc
            """)
    List<InfoItem> findByIsDeletedFalseOrderByPublishedAtDescCollectedAtDesc();

    @Query("""
            select infoItem
            from InfoItem infoItem
            where infoItem.isDeleted = false
              and infoItem.isHidden = false
              and infoItem.decisionStatus not in :decisionStatuses
            order by
                case when infoItem.publishedAt is null then 1 else 0 end asc,
                infoItem.publishedAt desc,
                infoItem.collectedAt desc,
                infoItem.id desc
            """)
    List<InfoItem> findVisibleByDecisionStatusNotInOrderByPublishedAtDescCollectedAtDesc(Collection<DecisionStatus> decisionStatuses);

    List<InfoItem> findByIsDeletedFalseAndManualOverrideFalseAndDecisionStatusOrderByCollectedAtDesc(DecisionStatus decisionStatus);

    long countByIsDeletedFalseAndDecisionStatus(DecisionStatus decisionStatus);

    long countByIsDeletedFalseAndIsHiddenFalseAndDecisionStatus(DecisionStatus decisionStatus);

    long countByIsDeletedFalseAndIsHiddenTrue();
}
