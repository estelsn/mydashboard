package com.aifomo.dashboard.service;

import com.aifomo.dashboard.dto.CollectionRunResponse;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionRunQueryService {

    private final CollectionRunRepository collectionRunRepository;

    @Transactional(readOnly = true)
    public List<CollectionRunResponse> findRecentRuns() {
        return collectionRunRepository.findTop10ByOrderByCreatedAtDescIdDesc().stream()
                .map(CollectionRunResponse::from)
                .toList();
    }

    @Transactional
    public void deleteRun(Long id) {
        if (!collectionRunRepository.existsById(id)) {
            throw new CollectionRunNotFoundException(id);
        }
        log.info("Collection run delete requested: id={}", id);
        collectionRunRepository.deleteById(id);
        log.info("Collection run deleted: id={}", id);
    }
}
