package com.aifomo.dashboard.service;

import com.aifomo.dashboard.dto.CollectionRunResponse;
import com.aifomo.dashboard.dto.CollectionSourceResultResponse;
import com.aifomo.dashboard.domain.collection.CollectionRun;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import com.aifomo.dashboard.repository.CollectionSourceResultRepository;
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
    private final CollectionSourceResultRepository collectionSourceResultRepository;

    @Transactional(readOnly = true)
    public List<CollectionRunResponse> findRecentRuns() {
        return collectionRunRepository.findTop10ByOrderByCreatedAtDescIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteRun(Long id) {
        if (!collectionRunRepository.existsById(id)) {
            throw new CollectionRunNotFoundException(id);
        }
        CollectionRun run = collectionRunRepository.findById(id)
                .orElseThrow(() -> new CollectionRunNotFoundException(id));
        log.info("Collection run delete requested: id={}", id);
        collectionSourceResultRepository.deleteByCollectionRun(run);
        collectionRunRepository.delete(run);
        log.info("Collection run deleted: id={}", id);
    }

    private CollectionRunResponse toResponse(CollectionRun run) {
        return CollectionRunResponse.from(
                run,
                collectionSourceResultRepository.findByCollectionRunOrderBySourcePriorityAscIdAsc(run).stream()
                        .map(CollectionSourceResultResponse::from)
                        .toList()
        );
    }
}
