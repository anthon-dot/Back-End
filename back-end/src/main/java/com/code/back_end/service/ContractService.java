// =======================================
// ContractService.java
// =======================================
package com.code.back_end.service;

import com.code.back_end.entity.Contract;
import com.code.back_end.entity.Occupant;
import com.code.back_end.entity.Stall;

import com.code.back_end.repository.ContractRepository;
import com.code.back_end.repository.StallRepository;
import com.code.back_end.security.SecurityService;

import com.code.back_end.repository.OccupantRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final StallRepository stallRepository;
    private final BillingService billingService;
    private final StakeholderService stakeholderService;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;
    private final OccupantRepository occupantRepo;

    public ContractService(
        ContractRepository contractRepository,
        StallRepository stallRepository,
        OccupantRepository occupantRepo,
        BillingService billingService,
        StakeholderService stakeholderService,
        SecurityService securityService,
        AuditLogService auditLogService
) {

    this.contractRepository =
            contractRepository;

    this.stallRepository =
            stallRepository;

    this.occupantRepo =
            occupantRepo;

    this.billingService =
            billingService;

    this.stakeholderService =
            stakeholderService;

    this.securityService =
            securityService;

    this.auditLogService =
            auditLogService;
}

    // =========================
    // GET ALL
    // =========================
    public List<Contract> getAllContracts() {

        if (securityService.canManageOperations()) {
            return contractRepository.findAll();
        }

        securityService.requireVerifiedStakeholderOwnerOrStaff(
                securityService.currentUser().getId()
        );

        return contractRepository
                .findByOccupant_Stakeholder_User_Id(
                        securityService.currentUser()
                                .getId()
                );
    }

    // =========================
    // GET BY ID
    // =========================
    public Optional<Contract> getContractById(
            Long id
    ) {

        if (securityService.canManageOperations()) {
            return contractRepository.findById(id);
        }

        securityService.requireVerifiedStakeholderOwnerOrStaff(
                securityService.currentUser().getId()
        );

        return contractRepository
                .findByIdAndOccupant_Stakeholder_User_Id(
                        id,
                        securityService.currentUser()
                                .getId()
                );
    }

    // =========================
    // CREATE CONTRACT
    // =========================
    public Contract createContract(
            Contract contract
    ) {

        securityService.requireSupervisorOrAdmin();

        // =========================
        // VALIDATE STALL
        // =========================
        if (contract.getStall() == null
                || contract.getStall().getId() == null) {

            throw new RuntimeException(
                    "Stall is required"
            );
        }

        // =========================
        // FETCH STALL
        // =========================
        Stall stall =
                stallRepository.findById(
                        contract.getStall().getId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Stall not found"
                        )
                );

        // =========================
        // CHECK OCCUPANT
        // =========================
        if (stall.getOccupant() == null) {

            throw new RuntimeException(
                    "Cannot create contract because stall is vacant"
            );
        }

        Occupant currentOccupant =
                stall.getOccupant();

        if (currentOccupant.getStakeholder() == null) {
            throw new RuntimeException(
                    "Assigned occupant has no stakeholder"
            );
        }

        if (!"APPROVED".equals(
                currentOccupant.getStakeholder()
                        .getMarketApprovalStatus()
        )) {
            throw new RuntimeException(
                    "Contract requires market supervisor approval"
            );
        }

        // =========================
        // VALIDATE OCCUPANT
        // =========================
        if (contract.getOccupant() == null
                || contract.getOccupant().getId() == null) {

            throw new RuntimeException(
                    "Occupant is required"
            );
        }

        // =========================
        // ENSURE OCCUPANT MATCHES
        // =========================
        if (!currentOccupant.getId().equals(
                contract.getOccupant().getId()
        )) {

            throw new RuntimeException(
                    "This occupant is not assigned to this stall"
            );
        }

        // =========================
        // VALIDATE DATES
        // =========================
        LocalDate startDate =
                contract.getStartDate();

        LocalDate endDate =
                contract.getEndDate();

        if (startDate == null
                || endDate == null) {

            throw new RuntimeException(
                    "Start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {

            throw new RuntimeException(
                    "End date cannot be before start date"
            );
        }

        // =========================
        // VALIDATE BILLING FREQUENCY
        // =========================
        List<String> frequencies =
                List.of(
                        "WEEKLY",
                        "SEMI_MONTHLY",
                        "15_DAYS",
                        "MONTHLY"
                );

        if (contract.getBillingFrequency() == null
                || !frequencies.contains(
                        contract.getBillingFrequency()
                                .toUpperCase()
                )) {

            throw new RuntimeException(
                    "Invalid billing frequency"
            );
        }

        // =========================
        // CHECK OVERLAPS
        // =========================
        List<Contract> existingContracts =
                contractRepository.findByStallId(
                        stall.getId()
                );

        for (Contract existing : existingContracts) {

            // SKIP CANCELLED
            if ("CANCELLED".equalsIgnoreCase(
                    existing.getStatus()
            )) {
                continue;
            }

            boolean overlaps =
                    !startDate.isAfter(
                            existing.getEndDate()
                    )
                    &&
                    !endDate.isBefore(
                            existing.getStartDate()
                    );

            if (overlaps) {

                throw new RuntimeException(
                        "Contract overlaps with existing contract: "
                                + existing.getContractNo()
                );
            }
        }

        // =========================
        // AUTO SET VALUES
        // =========================
        contract.setOccupant(
                currentOccupant
        );

        contract.setStall(
                stall
        );

        // DEFAULT STATUS
        if (contract.getStatus() == null
                || contract.getStatus().isBlank()) {

            contract.setStatus(
                    "DRAFT"
            );
        }

        // =========================
        // SAVE CONTRACT
        // =========================
       Contract saved =
        contractRepository.save(
                contract
        );

// =========================
// UPDATE OCCUPANT
// =========================
currentOccupant.setContractId(
        saved.getId()
);

// AUTO ACTIVATE OCCUPANT
currentOccupant.setStatus(
        "ACTIVE"
);

occupantRepo.save(
        currentOccupant
);

// =========================
// GENERATE INITIAL BILLING
// =========================
if ("ACTIVE".equalsIgnoreCase(
        saved.getStatus()
)) {

    billingService.generateInitialBilling(
            saved
    );
}

// =========================
// UPDATE ONBOARDING
// =========================
currentOccupant.getStakeholder()
        .setOnboardingStatus(
                "CONTRACT_CREATED"
        );

stakeholderService.refreshOnboardingStatus(
        currentOccupant.getStakeholder()
);

// =========================
// AUDIT LOG
// =========================
auditLogService.log(
        "CONTRACT_CREATED",
        "Contract",
        saved.getId(),
        "Contract created for stall "
                + stall.getId()
);

return saved;
    }
    // =========================
    // UPDATE CONTRACT
    // =========================
    public Contract updateContract(
            Long id,
            Contract updatedContract
    ) {

        securityService.requireSupervisorOrAdmin();

        Contract existing =
                contractRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Contract not found"
                                )
                        );

        LocalDate startDate =
                updatedContract.getStartDate();

        LocalDate endDate =
                updatedContract.getEndDate();

        if (startDate == null
                || endDate == null) {

            throw new RuntimeException(
                    "Start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {

            throw new RuntimeException(
                    "End date cannot be before start date"
            );
        }

        // =========================
        // CHECK OVERLAPS
        // =========================
        List<Contract> contracts =
                contractRepository.findByStallId(
                        existing.getStall().getId()
                );

        for (Contract contract : contracts) {

            // SKIP SELF
            if (contract.getId().equals(id)) {
                continue;
            }

            // SKIP CANCELLED
            if ("CANCELLED".equalsIgnoreCase(
                    contract.getStatus()
            )) {
                continue;
            }

            boolean overlaps =
                    !startDate.isAfter(
                            contract.getEndDate()
                    )
                    &&
                    !endDate.isBefore(
                            contract.getStartDate()
                    );

            if (overlaps) {

                throw new RuntimeException(
                        "Updated contract overlaps with contract: "
                                + contract.getContractNo()
                );
            }
        }

        existing.setContractNo(
                updatedContract.getContractNo()
        );

        existing.setStartDate(
                updatedContract.getStartDate()
        );

        existing.setEndDate(
                updatedContract.getEndDate()
        );

        existing.setMonthlyRent(
                updatedContract.getMonthlyRent()
        );

        existing.setBillingFrequency(
                updatedContract.getBillingFrequency()
        );

        existing.setTerms(
                updatedContract.getTerms()
        );

        existing.setStatus(
                updatedContract.getStatus()
        );

        Contract saved =
                contractRepository.save(
                existing
        );

        auditLogService.log(
                "CONTRACT_UPDATED",
                "Contract",
                saved.getId(),
                "Contract updated"
        );

        return saved;
    }

    // =========================
    // UPDATE STATUS
    // =========================
    public Contract updateContractStatus(
            Long id,
            String status
    ) {

        securityService.requireSupervisorOrAdmin();

        Contract contract =
                contractRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Contract not found"
                                )
                        );

        contract.setStatus(
                status.toUpperCase()
        );

        Contract saved =
                contractRepository.save(
                contract
        );

        auditLogService.log(
                "CONTRACT_STATUS_UPDATED",
                "Contract",
                saved.getId(),
                "Contract status updated to " + saved.getStatus()
        );

        return saved;
    }

    // =========================
    // DELETE CONTRACT
    // =========================
    public void deleteContract(
            Long id
    ) {

        securityService.requireAdmin();

        Contract contract =
                contractRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Contract not found"
                                )
                        );

        contractRepository.delete(
                contract
        );

        auditLogService.log(
                "CONTRACT_DELETED",
                "Contract",
                id,
                "Contract deleted"
        );
    }
}
