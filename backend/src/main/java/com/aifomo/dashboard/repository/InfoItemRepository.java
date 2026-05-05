package com.aifomo.dashboard.repository;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.info.InfoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InfoItemRepository extends JpaRepository<InfoItem, Long> {

    Optional<InfoItem> findByCollectedItem(CollectedItem collectedItem);
}
