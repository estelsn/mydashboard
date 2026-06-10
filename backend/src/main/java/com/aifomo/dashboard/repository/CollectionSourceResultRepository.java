package com.aifomo.dashboard.repository;

import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.domain.collection.CollectionSourceResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionSourceResultRepository extends JpaRepository<CollectionSourceResult, Long> {

    List<CollectionSourceResult> findByCollectionRunOrderBySourcePriorityAscIdAsc(CollectionRun collectionRun);

    void deleteByCollectionRun(CollectionRun collectionRun);
}
