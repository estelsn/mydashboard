package com.aifomo.dashboard.service;

import com.aifomo.dashboard.dto.CollectionRunResponse;
import com.aifomo.dashboard.repository.CollectionRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionRunQueryService {

    private final CollectionRunRepository collectionRunRepository;

    @Transactional(readOnly = true)
    public List<CollectionRunResponse> findRecentRuns() {
        return collectionRunRepository.findTop10ByOrderByCreatedAtDescIdDesc().stream()
                .map(CollectionRunResponse::from)
                .toList();
    }
}
