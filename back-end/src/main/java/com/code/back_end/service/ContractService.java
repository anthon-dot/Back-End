// =======================================
// ContractService.java
// =======================================
package com.code.back_end.service;

import com.code.back_end.entity.Contract;
import com.code.back_end.entity.Occupant;
import com.code.back_end.entity.PaymentType;
import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.Stall;
import com.code.back_end.entity.User;

import com.code.back_end.repository.BusinessApplicationRepository;
import com.code.back_end.repository.ContractRepository;
import com.code.back_end.repository.OccupantRepository;
import com.code.back_end.repository.PaymentRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.repository.StallRepository;
import com.code.back_end.repository.UserRepository;
import com.code.back_end.security.SecurityService;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContractService {

    private static final Logger log =
            LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final StallRepository stallRepository;
    private final BillingService billingService;
    private final StakeholderService stakeholderService;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;
    private final OccupantRepository occupantRepo;
    private final PaymentRepository paymentRepository;
    private final StakeholderRepository stakeholderRepository;
    private final BusinessApplicationRepository businessApplicationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ContractService(
            ContractRepository contractRepository,
            StallRepository stallRepository,
            OccupantRepository occupantRepo,
            BillingService billingService,
            StakeholderService stakeholderService,
            SecurityService securityService,
            AuditLogService auditLogService,
            PaymentRepository paymentRepository,
            StakeholderRepository stakeholderRepository,
            BusinessApplicationRepository businessApplicationRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.contractRepository = contractRepository;
        this.stallRepository = stallRepository;
        this.occupantRepo = occupantRepo;
        this.billingService = billingService;
        this.stakeholderService = stakeholderService;
        this.securityService = securityService;
        this.auditLogService = auditLogService;
        this.paymentRepository = paymentRepository;
        this.stakeholderRepository = stakeholderRepository;
        this.businessApplicationRepository = businessApplicationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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

// Maintain current status (or PENDING) - do not activate prematurely before approvals & fee
if (currentOccupant.getStatus() == null || currentOccupant.getStatus().isBlank()) {
    currentOccupant.setStatus("PENDING");
}

occupantRepo.save(
        currentOccupant
);

// =========================
// UPDATE ONBOARDING
// =========================
if (currentOccupant.getStakeholder() != null) {
    currentOccupant.getStakeholder().setOnboardingStatus("CONTRACT_CREATED");
    stakeholderService.refreshOnboardingStatus(currentOccupant.getStakeholder());
}

// =========================
// AUDIT LOG
// =========================
auditLogService.log(
        "CONTRACT_CREATED",
        "Contract",
        saved.getId(),
        "Contract created for stall " + stall.getId()
);

// ========================================================
// AUTOMATIC ACTIVATION RE-EVALUATION (Scenario B support)
// If fee and approvals are already completed, activate now!
// ========================================================
boolean activated = activateContractIfEligible(saved);
if (activated) {
    return contractRepository.findById(saved.getId()).orElse(saved);
}

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

    // =====================================================
    // CENTRALIZED CONTRACT ACTIVATION & BILLING GENERATION
    // =====================================================
    @Transactional
    public boolean activateContractIfEligible(Long contractId) {
        if (contractId == null) {
            return false;
        }
        Optional<Contract> contractOpt = contractRepository.findById(contractId);
        if (contractOpt.isEmpty()) {
            log.debug("[ContractActivation] Contract {} not found", contractId);
            return false;
        }
        return activateContractIfEligible(contractOpt.get());
    }

    @Transactional
    public boolean activateContractIfEligibleForStakeholder(Long stakeholderId) {
        if (stakeholderId == null) {
            return false;
        }
        Optional<Contract> contractOpt =
                contractRepository.findFirstByOccupant_Stakeholder_IdOrderByCreatedAtDesc(stakeholderId);
        if (contractOpt.isEmpty()) {
            log.debug("[ContractActivation] No contract found for stakeholder {}", stakeholderId);
            return false;
        }
        return activateContractIfEligible(contractOpt.get());
    }

    @Transactional
    public boolean activateContractIfEligible(Contract contract) {
        if (contract == null || contract.getId() == null) {
            log.debug("[ContractActivation] Contract is null or unsaved");
            return false;
        }

        // Scenario E: Cancelled, expired, or ineligible contracts must never be activated
        String currentStatus = contract.getStatus() == null ? "" : contract.getStatus().trim().toUpperCase();
        if ("CANCELLED".equals(currentStatus)
                || "CANCELED".equals(currentStatus)
                || "TERMINATED".equals(currentStatus)
                || "EXPIRED".equals(currentStatus)
                || "REJECTED".equals(currentStatus)) {
            log.debug("[ContractActivation] Contract {} is in status {} and ineligible for activation",
                    contract.getId(), currentStatus);
            return false;
        }

        // Validate contract dates
        LocalDate startDate = contract.getStartDate();
        LocalDate endDate = contract.getEndDate();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            log.debug("[ContractActivation] Contract {} has invalid dates (start={}, end={})",
                    contract.getId(), startDate, endDate);
            return false;
        }
        if (endDate.isBefore(LocalDate.now())) {
            log.debug("[ContractActivation] Contract {} has already expired on {}", contract.getId(), endDate);
            return false;
        }

        // Verify occupant, stall, and stakeholder relationships
        Occupant occupant = contract.getOccupant();
        if (occupant == null || occupant.getId() == null) {
            log.debug("[ContractActivation] Contract {} has no valid occupant", contract.getId());
            return false;
        }

        Stall stall = contract.getStall();
        if (stall == null || stall.getId() == null) {
            log.debug("[ContractActivation] Contract {} has no valid stall", contract.getId());
            return false;
        }

        Stakeholder stakeholder = occupant.getStakeholder();
        if (stakeholder == null || stakeholder.getId() == null) {
            Optional<Occupant> occOpt = occupantRepo.findById(occupant.getId());
            if (occOpt.isPresent() && occOpt.get().getStakeholder() != null) {
                stakeholder = occOpt.get().getStakeholder();
            } else {
                log.debug("[ContractActivation] Occupant {} has no associated stakeholder", occupant.getId());
                return false;
            }
        }

        // Verify Market Supervisor approval
        boolean marketApproved = Boolean.TRUE.equals(stakeholder.getMarketSupervisorApproved())
                || "APPROVED".equalsIgnoreCase(stakeholder.getMarketApprovalStatus());
        if (!marketApproved) {
            log.debug("[ContractActivation] Contract {} stakeholder {} missing Market Supervisor approval",
                    contract.getId(), stakeholder.getId());
            return false;
        }

        // Verify BPLO approval
        boolean bploApproved = Boolean.TRUE.equals(stakeholder.getBploApproved())
                || "APPROVED".equalsIgnoreCase(stakeholder.getBploStatus());
        if (!bploApproved) {
            log.debug("[ContractActivation] Contract {} stakeholder {} missing BPLO approval",
                    contract.getId(), stakeholder.getId());
            return false;
        }

        // Verify Endorsing Office approval
        boolean endorsingApproved = Boolean.TRUE.equals(stakeholder.getEndorsingApproved())
                || Boolean.TRUE.equals(stakeholder.getFinalEndorsed())
                || "APPROVED".equalsIgnoreCase(stakeholder.getEndorsementStatus())
                || "ENDORSED".equalsIgnoreCase(stakeholder.getEndorsingStatus());
        if (!endorsingApproved) {
            log.debug("[ContractActivation] Contract {} stakeholder {} missing Endorsing Office approval",
                    contract.getId(), stakeholder.getId());
            return false;
        }

        // Verify Application Fee / Business Permit Payment (backed by actual saved payment or fee record)
        boolean hasFeePayment = paymentRepository.findByStakeholderId(stakeholder.getId()).stream()
                .anyMatch(p -> (p.getPaymentType() == PaymentType.APPLICATION_FEE
                        || p.getPaymentType() == PaymentType.BUSINESS_PERMIT_PAYMENT
                        || p.getPaymentType() == PaymentType.APPLICATION_FORM)
                        && p.getAmount() != null && p.getAmount().compareTo(BigDecimal.ZERO) > 0);

        boolean feeRecorded = Boolean.TRUE.equals(stakeholder.getApplicantFeePaid()) || hasFeePayment;
        if (!feeRecorded) {
            log.debug("[ContractActivation] Contract {} stakeholder {} has not completed application fee / permit payment",
                    contract.getId(), stakeholder.getId());
            return false;
        }

        // Scenario D: Contract is ALREADY ACTIVE
        if ("ACTIVE".equalsIgnoreCase(currentStatus)) {
            log.debug("[ContractActivation] Contract {} is already ACTIVE. Verifying occupant and initial billing.",
                    contract.getId());
            if (!"ACTIVE".equalsIgnoreCase(occupant.getStatus())) {
                occupant.setStatus("ACTIVE");
                occupantRepo.save(occupant);
            }
            billingService.generateInitialBilling(contract);
            return true;
        }

        // ==========================================
        // ALL CONDITIONS MET: ACTIVATE CONTRACT & OCCUPANT
        // ==========================================
        contract.setStatus("ACTIVE");
        Contract savedContract = contractRepository.save(contract);

        occupant.setStatus("ACTIVE");
        occupant.setContractId(savedContract.getId());
        if (occupant.getOccupancyDate() == null) {
            occupant.setOccupancyDate(startDate != null ? startDate : LocalDate.now());
        }
        if (stakeholder.getAdvanceBalance() != null) {
            occupant.setAdvanceBalance(stakeholder.getAdvanceBalance());
        }
        occupantRepo.save(occupant);

        // Update stakeholder verification and onboarding statuses
        stakeholder.setVerifiedTenant(true);
        stakeholder.setVerifiedStakeholder(true);
        stakeholder.setVerified(true);
        if (stakeholder.getVerificationDate() == null) {
            stakeholder.setVerificationDate(LocalDateTime.now());
        }
        if (stakeholder.getApprovedOn() == null) {
            stakeholder.setApprovedOn(LocalDate.now());
        }
        stakeholder.setApplicationStatus("COMPLETED");
        stakeholder.setOnboardingStatus("APPROVED");
        stakeholder.setFinalStatus("APPROVED");
        stakeholder.setTreasurerPaid(true);
        stakeholderRepository.save(stakeholder);

        // Update user status and role if present
        if (stakeholder.getUser() != null) {
            User u = stakeholder.getUser();
            u.setRole("STAKEHOLDER");
            u.setStatus("ACTIVE");
            userRepository.save(u);
        }

        // Sync BusinessApplication record
        syncApplication(stakeholder);

        // Generate initial billing automatically
        billingService.generateInitialBilling(savedContract);

        // Audit log & notification
        auditLogService.log(
                "CONTRACT_ACTIVATED",
                "Contract",
                savedContract.getId(),
                "Contract automatically activated for stall " + stall.getId() + " occupant " + occupant.getId()
        );

        notificationService.createNotification(
                stakeholder,
                "Contract Activated",
                "Your market stall contract is now active. Your initial billing statement has been generated."
        );

        log.info("[ContractActivation] Contract {} activated successfully for occupant {} stall {}",
                savedContract.getId(), occupant.getId(), stall.getId());

        return true;
    }

    private void syncApplication(Stakeholder stakeholder) {
        if (stakeholder.getUser() == null || stakeholder.getUser().getId() == null) {
            return;
        }
        businessApplicationRepository.findByUser_Id(stakeholder.getUser().getId())
                .ifPresent(app -> {
                    app.setApplicationStatus(stakeholder.getApplicationStatus());
                    app.setOnboardingStatus(stakeholder.getOnboardingStatus());
                    app.setVerifiedApplication(true);
                    app.setApplicantFeePaid(stakeholder.getApplicantFeePaid());
                    app.setApplicantFeeAmount(stakeholder.getApplicantFeeAmount());
                    app.setApplicantFeeDate(stakeholder.getApplicantFeeDate());
                    app.setApprovedOn(stakeholder.getApprovedOn());
                    businessApplicationRepository.save(app);
                });
    }
}
