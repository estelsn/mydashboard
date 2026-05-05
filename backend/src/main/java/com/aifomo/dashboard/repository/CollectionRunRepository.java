package com.aifomo.dashboard.repository;

import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {

    List<CollectionRun> findTop10ByOrderByCreatedAtDescIdDesc();

    boolean existsByStatus(CollectionRunStatus status);
}
