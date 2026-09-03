package com.code.back_end.service;

import com.code.back_end.entity.StallType;
import com.code.back_end.exception.ResourceNotFoundException;
import com.code.back_end.repository.StallTypeRepository;
import com.code.back_end.security.SecurityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StallTypeService {

    private final StallTypeRepository repo;
    private final SecurityService securityService;

    public StallTypeService(StallTypeRepository repo, SecurityService securityService) {
        this.repo = repo;
        this.securityService = securityService;
    }

    public List<StallType> getAll() {
        securityService.requireAdmin();
        return repo.findAll();
    }

    public StallType create(StallType type) {
        securityService.requireAdmin();
        type.setId(null);
        return repo.save(type);
    }

    public StallType update(Long id, StallType payload) {
        securityService.requireAdmin();
        StallType existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stall type not found: " + id));
        if (payload.getName()        != null) existing.setName(payload.getName());
        if (payload.getDescription() != null) existing.setDescription(payload.getDescription());
        if (payload.getStatus()      != null) existing.setStatus(payload.getStatus());
        return repo.save(existing);
    }
}
