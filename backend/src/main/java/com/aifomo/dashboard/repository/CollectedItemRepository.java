package com.aifomo.dashboard.repository;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollectedItemRepository extends JpaRepository<CollectedItem, Long> {

    Optional<CollectedItem> findByContentHash(String contentHash);

    Optional<CollectedItem> findByRawUrl(String rawUrl);
}
