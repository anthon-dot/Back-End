package com.code.back_end.controller;

import com.code.back_end.dto.OccupantDTO;
import com.code.back_end.dto.StallAllocationRequest;

import com.code.back_end.dto.StallDTO;

import com.code.back_end.entity.Occupant;
import com.code.back_end.entity.Stall;

import com.code.back_end.service.OccupantService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/occupants")
public class OccupantController {

    private final OccupantService service;

    public OccupantController(
            OccupantService service
    ) {

        this.service = service;
    }

    // =========================
    // GET ALL OCCUPANTS
    // =========================
    @GetMapping
    public List<OccupantDTO> getAll() {

        return service.getAll()
                .stream()
                .map(OccupantDTO::new)
                .collect(Collectors.toList());
    }

    // =========================
    // ALLOCATE STALL
    // =========================
    @PostMapping("/allocate/{stallId}")
    public StallDTO allocate(
            @PathVariable Long stallId,
            @RequestBody StallAllocationRequest request
    ) {

        Stall stall =
                service.allocateStall(
                        stallId,
                        request.getStakeholderId()
                );

        return new StallDTO(stall);
    }

    // =========================
    // VACATE STALL
    // =========================
    @PutMapping("/vacate/{stallId}")
    public StallDTO vacate(
            @PathVariable Long stallId
    ) {

        Stall stall =
                service.vacateStall(stallId);

        return new StallDTO(stall);
    }
}