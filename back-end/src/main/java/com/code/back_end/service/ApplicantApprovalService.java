package com.code.back_end.service;

import com.code.back_end.entity.Contract;
import com.code.back_end.entity.Occupant;
import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.StakeholderDocument;
import com.code.back_end.repository.BusinessApplicationRepository;
import com.code.back_end.repository.ContractRepository;
import com.code.back_end.repository.OccupantRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.security.SecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicantApprovalService {

    private final StakeholderRepository stakeholderRepo;
    private final BusinessApplicationRepository applicationRepo;
    private final OccupantRepository occupantRepo;
    private final ContractRepository contractRepo;
    private final BillingService billingService;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public ApplicantApprovalService(
            StakeholderRepository stakeholderRepo,
            BusinessApplicationRepository applicationRepo,
            OccupantRepository occupantRepo,
            ContractRepository contractRepo,
            BillingService billingService,
            SecurityService securityService,
            AuditLogService auditLogService,
            NotificationService notificationService
    ) {
        this.stakeholderRepo = stakeholderRepo;
        this.applicationRepo = applicationRepo;
        this.occupantRepo = occupantRepo;
        this.contractRepo = contractRepo;
        this.billingService = billingService;
        this.securityService = securityService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    private Stakeholder findForApproval(Long id) {
        return stakeholderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stakeholder not found"));
    }

    public Stakeholder approveMarketSupervisor(Long id) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder = findForApproval(id);
        if (!Boolean.TRUE.equals(stakeholder.getAdvancePaymentPaid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Advance payment must be paid before market approval"
            );
        }

        stakeholder.setMarketApprovalStatus("APPROVED");
        updateApplicationStatusFromStages(stakeholder);

        Stakeholder saved = refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_SUPERVISOR_APPROVED",
                "Stakeholder",
                saved.getId(),
                "Market supervisor approved stakeholder"
        );

        return saved;
    }

    public Stakeholder rejectMarketSupervisor(Long id) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder = findForApproval(id);
        stakeholder.setMarketApprovalStatus("REJECTED");
        stakeholder.setEndorsementStatus("PENDING");
        stakeholder.setBploStatus("PENDING");
        updateApplicationStatusFromStages(stakeholder);

        Stakeholder saved = refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_SUPERVISOR_REJECTED",
                "Stakeholder",
                saved.getId(),
                "Market supervisor rejected stakeholder"
        );

        return saved;
    }

    public Stakeholder approveBplo(Long id) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder = findForApproval(id);
        if (!contractRepo.existsByOccupant_Stakeholder_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "BPLO approval requires an existing contract"
            );
        }

        stakeholder.setBploStatus("APPROVED");
        updateApplicationStatusFromStages(stakeholder);

        Stakeholder saved = refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_BPLO_APPROVED",
                "Stakeholder",
                saved.getId(),
                "BPLO approved stakeholder"
        );

        return saved;
    }

    public Stakeholder rejectBplo(Long id) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder = findForApproval(id);
        if (!contractRepo.existsByOccupant_Stakeholder_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "BPLO rejection requires an existing contract"
            );
        }

        stakeholder.setBploStatus("REJECTED");
        updateApplicationStatusFromStages(stakeholder);

        Stakeholder saved = refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_BPLO_REJECTED",
                "Stakeholder",
                saved.getId(),
                "BPLO rejected stakeholder"
        );

        return saved;
    }

    public Stakeholder approveEndorsing(Long id) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder = findForApproval(id);
        if (!"APPROVED".equals(stakeholder.getBploStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Endorsement requires BPLO approval"
            );
        }

        stakeholder.setEndorsementStatus("APPROVED");
        stakeholder.setEndorsementRemarks(null);
        stakeholder.setEndorsedAt(LocalDateTime.now());
        updateApplicationStatusFromStages(stakeholder);

        Stakeholder saved = refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_ENDORSING_APPROVED",
                "Stakeholder",
                saved.getId(),
                "Endorsing office approved stakeholder"
        );

        return saved;
    }

    public Stakeholder rejectEndorsing(Long id, String remarks) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder = findForApproval(id);
        if (!"APPROVED".equals(stakeholder.getBploStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Endorsement rejection requires BPLO approval"
            );
        }

        stakeholder.setEndorsementStatus("REJECTED");
        stakeholder.setEndorsementRemarks(remarks);
        stakeholder.setBploStatus("PENDING");
        updateApplicationStatusFromStages(stakeholder);

        Stakeholder saved = refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_ENDORSING_REJECTED",
                "Stakeholder",
                saved.getId(),
                "Endorsing office rejected stakeholder"
        );

        return saved;
    }

    public Stakeholder rejectEndorsing(Long id) {
        return rejectEndorsing(id, null);
    }

    public Stakeholder payApplicantFee(Long id, BigDecimal amount) {
        securityService.requireSupervisorOrAdmin();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Applicant fee amount must be greater than zero"
            );
        }

        Stakeholder stakeholder = findForApproval(id);
        if (!"APPROVED".equals(stakeholder.getEndorsementStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Applicant fee can only be paid after endorsement"
            );
        }

        stakeholder.setApplicantFeePaid(true);
        stakeholder.setApplicantFeeAmount(amount);
        stakeholder.setApplicantFeeDate(LocalDate.now());
        stakeholder.setVerified(true);
        stakeholder.setVerificationDate(LocalDateTime.now());

        if (
                "APPROVED".equals(stakeholder.getFinalStatus())
                        || "PENDING_BUSINESS_PERMIT_PAYMENT".equals(stakeholder.getApplicationStatus())
                        || "APPROVED".equals(stakeholder.getApplicationStatus())
                        || "FULLY_APPROVED".equals(stakeholder.getApplicationStatus())
        ) {
            stakeholder.setApplicationStatus("COMPLETED");
            stakeholder.setOnboardingStatus("APPROVED");
            stakeholder.setTreasurerPaid(true);
        }

        Stakeholder saved = refreshOnboardingStatus(stakeholder);

        notificationService.createNotification(
                saved,
                "Business Permit Payment Confirmed",
                "Your business permit payment has been confirmed. Your stakeholder account is now active."
        );

        auditLogService.log(
                "STAKEHOLDER_BUSINESS_PERMIT_PAID",
                "Stakeholder",
                saved.getId(),
                "Business permit payment paid"
        );

        return saved;
    }

    public Stakeholder approve(Long id) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder = findForApproval(id);
        if (!"FOR_APPROVAL".equals(stakeholder.getOnboardingStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Stakeholder must be ready for approval"
            );
        }

        stakeholder.setMarketApprovalStatus("APPROVED");
        stakeholder.setEndorsementStatus("APPROVED");
        stakeholder.setBploStatus("APPROVED");
        stakeholder.setApplicationStatus(
                Boolean.TRUE.equals(stakeholder.getApplicantFeePaid()) ? "FULLY_APPROVED" : "APPROVED"
        );
        stakeholder.setOnboardingStatus("APPROVED");
        stakeholder.setVerifiedStakeholder(stakeholder.getApplicantFeePaid());
        stakeholder.setVerifiedTenant(stakeholder.getApplicantFeePaid());

        if (stakeholder.getApprovedOn() == null) {
            stakeholder.setApprovedOn(LocalDate.now());
        }

        Stakeholder saved = refreshOnboardingStatus(stakeholder);

        if (Boolean.TRUE.equals(saved.getVerifiedTenant())) {
            activateOccupantAndBilling(saved);
        }

        auditLogService.log(
                "STAKEHOLDER_APPROVED",
                "Stakeholder",
                saved.getId(),
                "Stakeholder application approved"
        );

        return saved;
    }

    public Stakeholder reject(Long id) {
        return reject(id, null);
    }

    public Stakeholder reject(Long id, String remarks) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder = findForApproval(id);
        stakeholder.setApplicationStatus("REJECTED");
        stakeholder.setOnboardingStatus("REJECTED");
        stakeholder.setMarketApprovalStatus("REJECTED");
        stakeholder.setEndorsementStatus("PENDING");
        stakeholder.setBploStatus("PENDING");
        stakeholder.setRemarks(remarks);
        stakeholder.setNotes(remarks);

        Stakeholder saved = refreshOnboardingStatus(stakeholder);

        notificationService.createNotification(
                saved,
                "Application Rejected",
                remarks == null || remarks.isBlank()
                        ? "Your stall application was rejected."
                        : "Your stall application was rejected: " + remarks
        );

        auditLogService.log(
                "STAKEHOLDER_REJECTED",
                "Stakeholder",
                saved.getId(),
                remarks == null || remarks.isBlank()
                        ? "Stakeholder application rejected"
                        : "Stakeholder application rejected: " + remarks
        );

        return saved;
    }

    public void updateOverallStatus(Stakeholder stakeholder) {
        List<StakeholderDocument> docs = stakeholder.getDocuments();
        if (docs == null) {
            docs = List.of();
        }

        boolean hasLetter = docs.stream().anyMatch(doc -> "LETTER".equals(doc.getDocumentType()));
        boolean hasId = docs.stream().anyMatch(doc -> "ID".equals(doc.getDocumentType()));

        if (hasLetter && hasId) {
            stakeholder.updateVerificationStatus();
        }

        stakeholderRepo.save(stakeholder);

        if (Boolean.TRUE.equals(stakeholder.getVerifiedTenant())) {
            boolean exists = occupantRepo.existsByStakeholder_Id(stakeholder.getId());
            if (!exists) {
                Occupant occupant = new Occupant();
                occupant.setStakeholder(stakeholder);
                occupantRepo.save(occupant);
            }
        }
    }

    public void updateApplicationStatusFromStages(Stakeholder stakeholder) {
        if (
                "REJECTED".equals(stakeholder.getMarketApprovalStatus())
                        || "REJECTED".equals(stakeholder.getEndorsementStatus())
                        || "REJECTED".equals(stakeholder.getBploStatus())
        ) {
            stakeholder.setApplicationStatus("REJECTED");
            stakeholder.setVerifiedTenant(false);
            return;
        }

        if (
                "APPROVED".equals(stakeholder.getApplicationStatus())
                        || "REJECTED".equals(stakeholder.getApplicationStatus())
        ) {
            stakeholder.setApplicationStatus("PENDING");
        }

        stakeholder.updateVerificationStatus();
    }

    public Stakeholder refreshOnboardingStatus(Stakeholder stakeholder) {
        if (stakeholder == null || stakeholder.getId() == null) {
            return stakeholder;
        }

        boolean rejected =
                "REJECTED".equals(stakeholder.getApplicationStatus())
                        || "REJECTED".equals(stakeholder.getOnboardingStatus())
                        || "REJECTED".equals(stakeholder.getMarketApprovalStatus())
                        || "REJECTED".equals(stakeholder.getEndorsementStatus())
                        || "REJECTED".equals(stakeholder.getBploStatus());

        if (rejected) {
            stakeholder.setApplicationStatus("REJECTED");
            stakeholder.setOnboardingStatus("REJECTED");
            stakeholder.setVerifiedStakeholder(false);
            stakeholder.setVerifiedTenant(false);
        } else if ("COMPLETED".equals(stakeholder.getApplicationStatus())) {
            stakeholder.setOnboardingStatus("APPROVED");
            stakeholder.setVerifiedStakeholder(stakeholder.getApplicantFeePaid());
            stakeholder.setVerifiedTenant(stakeholder.getApplicantFeePaid());

            if (stakeholder.getApprovedOn() == null) {
                stakeholder.setApprovedOn(LocalDate.now());
            }
        } else if (
                "APPROVED".equals(stakeholder.getApplicationStatus())
                        || "FULLY_APPROVED".equals(stakeholder.getApplicationStatus())
        ) {
            stakeholder.setApplicationStatus(
                    Boolean.TRUE.equals(stakeholder.getApplicantFeePaid())
                            ? "FULLY_APPROVED"
                            : "APPROVED"
            );
            stakeholder.setOnboardingStatus("APPROVED");
            stakeholder.setVerifiedStakeholder(stakeholder.getApplicantFeePaid());
            stakeholder.setVerifiedTenant(stakeholder.getApplicantFeePaid());

            if (stakeholder.getApprovedOn() == null) {
                stakeholder.setApprovedOn(LocalDate.now());
            }
        } else if (
                Boolean.TRUE.equals(stakeholder.getAdvancePaymentPaid())
                        || Boolean.TRUE.equals(stakeholder.getAdvancePaymentCompleted())
        ) {
            stakeholder.setApplicationStatus("PENDING");
            stakeholder.setOnboardingStatus("FOR_APPROVAL");
            stakeholder.setVerifiedStakeholder(false);
            stakeholder.setVerifiedTenant(false);
        } else if ("NEW".equals(stakeholder.getOnboardingStatus())) {
            stakeholder.setApplicationStatus("PENDING");
            stakeholder.setOnboardingStatus("PAYMENT_PENDING");
        }

        Stakeholder saved = stakeholderRepo.save(stakeholder);
        syncApplicationFromStakeholder(saved);
        return saved;
    }

    public Stakeholder refreshOnboardingStatus(Long stakeholderId) {
        Stakeholder stakeholder = findForApproval(stakeholderId);
        return refreshOnboardingStatus(stakeholder);
    }

    public void syncApplicationFromStakeholder(Stakeholder stakeholder) {
        if (stakeholder.getUser() == null || stakeholder.getUser().getId() == null) {
            return;
        }

        applicationRepo.findByUser_Id(stakeholder.getUser().getId())
                .ifPresent(application -> {
                    application.setApplicationStatus(stakeholder.getApplicationStatus());
                    application.setOnboardingStatus(stakeholder.getOnboardingStatus());
                    application.setAdvancePaymentPaid(stakeholder.getAdvancePaymentPaid());
                    application.setAdvancePaymentCompleted(stakeholder.getAdvancePaymentCompleted());
                    application.setAdvancePaymentAmount(stakeholder.getAdvancePaymentAmount());
                    application.setAdvanceBalance(stakeholder.getAdvanceBalance());
                    application.setTotalAdvanceAmount(stakeholder.getTotalAdvanceAmount());
                    application.setAdvancePaymentDate(stakeholder.getAdvancePaymentDate());
                    application.setMarketApprovalStatus(stakeholder.getMarketApprovalStatus());
                    application.setBploStatus(stakeholder.getBploStatus());
                    application.setEndorsementStatus(stakeholder.getEndorsementStatus());
                    application.setEndorsingStatus(stakeholder.getEndorsingStatus());
                    application.setFinalStatus(stakeholder.getFinalStatus());
                    application.setEndorsementRemarks(stakeholder.getEndorsementRemarks());
                    application.setEndorsedAt(stakeholder.getEndorsedAt());
                    application.setEndorsedBy(stakeholder.getEndorsedBy());
                    application.setBploApprovedBy(stakeholder.getBploApprovedBy());
                    application.setApprovalDate(stakeholder.getApprovalDate());
                    application.setRemarks(stakeholder.getRemarks());
                    application.setSelectedStall(stakeholder.getSelectedStall());
                    application.setApplicantFeePaid(stakeholder.getApplicantFeePaid());
                    application.setApplicantFeeAmount(stakeholder.getApplicantFeeAmount());
                    application.setApplicantFeeDate(stakeholder.getApplicantFeeDate());
                    application.setVerifiedApplication(stakeholder.getVerifiedStakeholder());
                    application.setApprovedOn(stakeholder.getApprovedOn());
                    applicationRepo.save(application);
                });
    }

    public void activateOccupantAndBilling(Stakeholder stakeholder) {
        Occupant occupant = occupantRepo.findByStakeholder_IdAndIsArchivedFalse(stakeholder.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stakeholder has no assigned stall"));

        Contract contract = contractRepo.findFirstByOccupant_Stakeholder_IdOrderByCreatedAtDesc(stakeholder.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stakeholder has no contract"));

        occupant.setStatus("ACTIVE");
        if (occupant.getOccupancyDate() == null) {
            occupant.setOccupancyDate(LocalDate.now());
        }

        occupant.setContractId(contract.getId());
        occupant.setAdvanceBalance(
                stakeholder.getAdvanceBalance() == null ? BigDecimal.ZERO : stakeholder.getAdvanceBalance()
        );
        occupantRepo.save(occupant);

        if (!"ACTIVE".equalsIgnoreCase(contract.getStatus())) {
            contract.setStatus("ACTIVE");
            contractRepo.save(contract);
        }

        billingService.generateInitialBilling(contract);
    }
}
