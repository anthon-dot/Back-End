package com.code.back_end.service;

import com.code.back_end.entity.Occupant;
import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.Stall;

import com.code.back_end.repository.OccupantRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.repository.StallRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OccupantService {

    private static final Logger log =
            LoggerFactory.getLogger(OccupantService.class);

    private final OccupantRepository occupantRepo;
    private final StakeholderRepository stakeholderRepo;
    private final StallRepository stallRepo;
    private final StakeholderService stakeholderService;

    public OccupantService(
            OccupantRepository occupantRepo,
            StakeholderRepository stakeholderRepo,
            StallRepository stallRepo,
            StakeholderService stakeholderService
    ) {

        this.occupantRepo = occupantRepo;
        this.stakeholderRepo = stakeholderRepo;
        this.stallRepo = stallRepo;
        this.stakeholderService = stakeholderService;
    }

    // =========================
    // GET ALL OCCUPANTS
    // =========================
    public List<Occupant> getAll() {
        return occupantRepo.findAll();
    }

   // =========================
// ALLOCATE STALL
// =========================
public Stall allocateStall(
        Long stallId,
        Long stakeholderId
) {

    // FIND STALL
    Stall stall = stallRepo.findById(stallId)
            .orElseThrow(() ->
                    new RuntimeException(
                            "Stall not found"
                    )
            );

    // FIND STAKEHOLDER
    Stakeholder stakeholder =
            stakeholderRepo.findById(
                    stakeholderId
            )
            .orElseThrow(() ->
                    new RuntimeException(
                            "Stakeholder not found"
                    )
            );

    // =========================
    // APPROVAL CHECK
    // advancePayment = true
    // marketSupervisorApproved = true
    // =========================
    boolean approved =
            Boolean.TRUE.equals(
                    stakeholder.getAdvancePaymentPaid()
            )
            &&
            "APPROVED".equals(
                    stakeholder.getMarketApprovalStatus()
            );

    if (!approved) {

        throw new RuntimeException(
                "Stakeholder must have paid advance payment and be market approved"
        );
    }

    // =========================
    // CHECK IF STALL OCCUPIED
    // =========================
    if (stall.getOccupant() != null) {

        throw new RuntimeException(
                "Stall already occupied"
        );
    }

    // =========================
    // PREVENT DUPLICATE OCCUPANT
    // =========================
    Occupant existingOccupant =
            occupantRepo.findAll()
                    .stream()
                    .filter(o ->
                            o.getStakeholder() != null
                            &&
                            o.getStakeholder()
                                    .getId()
                                    .equals(
                                            stakeholder.getId()
                                    )
                            &&
                            Boolean.FALSE.equals(
                                    o.getIsArchived()
                            )
                    )
                    .findFirst()
                    .orElse(null);

    Occupant occupant;

    if (existingOccupant != null) {

        occupant = existingOccupant;

    } else {

        // CREATE OCCUPANT
        occupant = new Occupant();

        occupant.setStakeholder(
                stakeholder
        );

        occupant.setOccupiedSince(
                LocalDateTime.now()
        );

        occupant.setOccupancyDate(
                LocalDate.now()
        );

        occupant.setAdvanceBalance(
                stakeholder.getAdvanceBalance() == null
                        ? BigDecimal.ZERO
                        : stakeholder.getAdvanceBalance()
        );

        occupant.setIsArchived(
                false
        );

        occupant.setStatus(
                "PENDING"
        );

        occupant =
                occupantRepo.save(
                        occupant
                );

        log.debug(
                "[Occupant] created occupant {} for stakeholder {} advanceBalance {} occupancyDate {} contractId {}",
                occupant.getId(),
                stakeholder.getId(),
                occupant.getAdvanceBalance(),
                occupant.getOccupancyDate(),
                occupant.getContractId()
        );
    }

    // =========================
    // ASSIGN TO STALL
    // =========================
    stall.setOccupant(
            occupant
    );

    stall.setStatus(
            "OCCUPIED"
    );

    Stall saved =
            stallRepo.save(
            stall
    );

    stakeholder.setOnboardingStatus("STALL_ASSIGNED");
    stakeholderRepo.save(stakeholder);
    stakeholderService.refreshOnboardingStatus(stakeholder);

    return saved;
}

    // =========================
    // VACATE STALL
    // =========================
    public Stall vacateStall(Long stallId) {

        Stall stall = stallRepo.findById(stallId)
                .orElseThrow(() ->
                        new RuntimeException("Stall not found"));

        Occupant occupant = stall.getOccupant();

        if (occupant == null) {

            throw new RuntimeException(
                    "Stall is already vacant"
            );
        }

        // ARCHIVE OCCUPANT
        occupant.setIsArchived(true);

        occupantRepo.save(occupant);

        // REMOVE OCCUPANT
        stall.setOccupant(null);

        stall.setStatus("VACANT");

        return stallRepo.save(stall);
    }
}
