package com.aifomo.dashboard.repository;

import com.aifomo.dashboard.domain.collected.CollectedItem;
import com.aifomo.dashboard.domain.source.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollectedItemRepository extends JpaRepository<CollectedItem, Long> {

    Optional<CollectedItem> findByContentHash(String contentHash);

    Optional<CollectedItem> findByRawUrl(String rawUrl);

    boolean existsBySource(Source source);
}
