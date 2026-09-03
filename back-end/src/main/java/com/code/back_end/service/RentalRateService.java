package com.code.back_end.service;

import com.code.back_end.entity.RentalRate;
import com.code.back_end.exception.ResourceNotFoundException;
import com.code.back_end.repository.RentalRateRepository;
import com.code.back_end.security.SecurityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RentalRateService {

    private final RentalRateRepository repo;
    private final SecurityService securityService;

    public RentalRateService(RentalRateRepository repo, SecurityService securityService) {
        this.repo = repo;
        this.securityService = securityService;
    }

    public List<RentalRate> getAll() {
        securityService.requireAdmin();
        return repo.findAll();
    }

    public RentalRate create(RentalRate rate) {
        securityService.requireAdmin();
        rate.setId(null);
        return repo.save(rate);
    }

    public RentalRate update(Long id, RentalRate payload) {
        securityService.requireAdmin();
        RentalRate existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental rate not found: " + id));
        if (payload.getStallType()  != null) existing.setStallType(payload.getStallType());
        if (payload.getMonthlyRate() != null) existing.setMonthlyRate(payload.getMonthlyRate());
        if (payload.getDescription() != null) existing.setDescription(payload.getDescription());
        if (payload.getStatus()      != null) existing.setStatus(payload.getStatus());
        return repo.save(existing);
    }
}
