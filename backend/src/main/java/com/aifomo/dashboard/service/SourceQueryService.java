package com.aifomo.dashboard.service;

import com.aifomo.dashboard.dto.SourceResponse;
import com.aifomo.dashboard.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SourceQueryService {

    private final SourceRepository sourceRepository;

    public List<SourceResponse> findAll() {
        return sourceRepository.findAllByOrderByPriorityAscIdAsc().stream()
                .map(SourceResponse::from)
                .toList();
    }
}
