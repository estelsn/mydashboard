package com.aifomo.dashboard.repository;

import com.aifomo.dashboard.domain.source.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SourceRepository extends JpaRepository<Source, Long> {

    Optional<Source> findByUrl(String url);
}
