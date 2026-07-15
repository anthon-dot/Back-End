package com.code.back_end.controller;

import com.code.back_end.entity.BusinessApplication;
import com.code.back_end.service.ApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    public BusinessApplication create(
            @RequestParam Long userId,
            @RequestParam String businessName,
            @RequestParam String businessType,
            @RequestParam String firstName,
            @RequestParam(required = false) String middleName,
            @RequestParam String lastName,
            @RequestParam String contact,
            @RequestParam String email,
            @RequestParam String address,
            @RequestParam Long selectedStallId,
            @RequestParam MultipartFile idFile,
            @RequestParam MultipartFile letterFile
    ) throws IOException {
        return service.create(
                userId,
                businessName,
                businessType,
                firstName,
                middleName,
                lastName,
                contact,
                email,
                address,
                selectedStallId,
                idFile,
                letterFile
        );
    }

    @GetMapping
    public List<BusinessApplication> getAll() {
        return service.getAll();
    }

    @GetMapping("/user/{userId}")
    public BusinessApplication getByUserId(
            @PathVariable Long userId
    ) {
        return service.getByUserId(userId);
    }

    @GetMapping("/{id}")
    public BusinessApplication getById(
            @PathVariable Long id
    ) {
        return service.getById(id);
    }

    @PutMapping("/{id}/approve")
    public BusinessApplication approve(
            @PathVariable Long id
    ) {
        return service.approve(id);
    }

    @PutMapping("/{id}/reject")
    public BusinessApplication reject(
            @PathVariable Long id
    ) {
        return service.reject(id);
    }

    @PutMapping("/{id}/endorse")
    @PreAuthorize("hasAnyRole('ENDORSING_OFFICE', 'ENDORISING_OFFICE')")
    public BusinessApplication endorse(
            @PathVariable Long id
    ) {
        return service.endorse(id);
    }

    @PutMapping("/{id}/endorse-reject")
    @PreAuthorize("hasAnyRole('ENDORSING_OFFICE', 'ENDORISING_OFFICE')")
    public BusinessApplication rejectEndorsement(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks
    ) {
        return service.rejectEndorsement(id, remarks);
    }

    @PutMapping("/{id}/bplo-approve")
    @PreAuthorize("hasRole('BPLO_OFFICE')")
    public BusinessApplication approveByBplo(
            @PathVariable Long id
    ) {
        return service.approveByBplo(id);
    }

    @PutMapping("/{id}/bplo-reject")
    @PreAuthorize("hasRole('BPLO_OFFICE')")
    public BusinessApplication rejectByBplo(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks
    ) {
        return service.rejectByBplo(id, remarks);
    }
}
