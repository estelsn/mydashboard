package com.aifomo.dashboard.service;

import com.aifomo.dashboard.domain.source.Source;
import com.aifomo.dashboard.domain.source.RetiredSourcePolicy;
import com.aifomo.dashboard.dto.SourceResponse;
import com.aifomo.dashboard.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceQueryService {

    private final SourceRepository sourceRepository;

    @Transactional(readOnly = true)
    public List<SourceResponse> findAll() {
        return sourceRepository.findAllByOrderByPriorityAscIdAsc().stream()
                .filter(source -> !RetiredSourcePolicy.isRetired(source))
                .map(SourceResponse::from)
                .toList();
    }

    @Transactional
    public SourceResponse updateEnabled(Long id, boolean enabled) {
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new SourceNotFoundException(id));
        if (RetiredSourcePolicy.isRetired(source)) {
            throw new SourceNotFoundException(id);
        }
        source.setEnabled(enabled);
        return SourceResponse.from(source);
    }
}
