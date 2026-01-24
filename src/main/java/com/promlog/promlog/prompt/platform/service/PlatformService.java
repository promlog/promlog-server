package com.promlog.promlog.prompt.platform.service;

import com.promlog.promlog.prompt.platform.dto.PlatformResponse;
import com.promlog.promlog.prompt.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlatformService {

    private final PlatformRepository platformRepository;

    public PlatformService(PlatformRepository platformRepository) {
        this.platformRepository = platformRepository;
    }

    @Transactional(readOnly = true)
    public List<PlatformResponse> list() {
        return platformRepository.findByDeletedAtIsNullOrderByNameAsc().stream()
                .map(PlatformResponse::from)
                .toList();
    }
}
