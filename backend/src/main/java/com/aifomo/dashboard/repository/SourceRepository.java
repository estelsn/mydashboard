package com.aifomo.dashboard.repository;

import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SourceRepository extends JpaRepository<Source, Long> {

    List<Source> findAllByOrderByPriorityAscIdAsc();

    List<Source> findByEnabledTrueAndSourceTypeInOrderByPriorityAscIdAsc(List<SourceType> sourceTypes);

    Optional<Source> findByUrl(String url);
}
