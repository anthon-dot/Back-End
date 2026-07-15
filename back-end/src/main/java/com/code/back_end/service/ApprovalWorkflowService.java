package com.code.back_end.service;

import com.code.back_end.dto.ApplicantFeeRequest;
import com.code.back_end.dto.RequirementStatusResponse;
import com.code.back_end.dto.StallAssignmentRequest;
import com.code.back_end.dto.TreasurerApprovalRequest;
import com.code.back_end.entity.*;
import com.code.back_end.repository.*;
import com.code.back_end.security.SecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ApprovalWorkflowService {

    private static final List<String> REQUIRED_DOCUMENTS =
            List.of("DTI_PERMIT", "CEDULA", "BARANGAY_CLEARANCE", "VALID_ID");

    private final StakeholderRepository stakeholderRepository;
    private final StakeholderDocumentRepository documentRepository;
    private final PaymentRepository paymentRepository;
    private final StallRepository stallRepository;
    private final OccupantRepository occupantRepository;
    private final ContractRepository contractRepository;
    private final BusinessApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final NotificationService notificationService;
    private final BillingService billingService;
    private final AuditLogService auditLogService;
    private final SecurityService securityService;

    public ApprovalWorkflowService(
            StakeholderRepository stakeholderRepository,
            StakeholderDocumentRepository documentRepository,
            PaymentRepository paymentRepository,
            StallRepository stallRepository,
            OccupantRepository occupantRepository,
            ContractRepository contractRepository,
            BusinessApplicationRepository applicationRepository,
            UserRepository userRepository,
            ApprovalHistoryRepository approvalHistoryRepository,
            NotificationService notificationService,
            BillingService billingService,
            AuditLogService auditLogService,
            SecurityService securityService
    ) {
        this.stakeholderRepository = stakeholderRepository;
        this.documentRepository = documentRepository;
        this.paymentRepository = paymentRepository;
        this.stallRepository = stallRepository;
        this.occupantRepository = occupantRepository;
        this.contractRepository = contractRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.notificationService = notificationService;
        this.billingService = billingService;
        this.auditLogService = auditLogService;
        this.securityService = securityService;
    }

    public Stakeholder approveByTreasurer(
            Long stakeholderId,
            TreasurerApprovalRequest request
    ) {
        securityService.requireTreasurerOrAdmin();

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Advance payment amount is required");
        }

        Stakeholder stakeholder = getStakeholder(stakeholderId);

        if (Boolean.TRUE.equals(stakeholder.getTreasurerApproved())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Treasurer approval already recorded");
        }

        BigDecimal totalAdvance =
                request.getTotalAdvanceAmount() == null
                        ? request.getAmount()
                        : request.getTotalAdvanceAmount();

        Payment payment = new Payment();
        payment.setStakeholder(stakeholder);
        payment.setAmount(request.getAmount());
        payment.setTotalAdvanceAmount(totalAdvance);
        payment.setPaymentType(PaymentType.ADVANCE_PAYMENT);
        payment.setReferenceNo(request.getReferenceNo());
        payment.setReceiptNo(generateReceiptNo("ADV"));
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        BigDecimal currentCredit =
                stakeholder.getAdvanceBalance() == null
                        ? BigDecimal.ZERO
                        : stakeholder.getAdvanceBalance();

        stakeholder.setTreasurerApproved(true);
        stakeholder.setAdvancePaymentPaid(true);
        stakeholder.setAdvancePaymentCompleted(true);
        stakeholder.setAdvancePaymentDate(LocalDate.now());
        stakeholder.setTotalAdvanceAmount(totalAdvance);
        stakeholder.setAdvancePaymentAmount(currentCredit.add(request.getAmount()));
        stakeholder.setAdvanceBalance(currentCredit.add(request.getAmount()));
        stakeholder.setApplicationStatus("PENDING_MARKET_SUPERVISOR_APPROVAL");
        stakeholder.setOnboardingStatus("FOR_APPROVAL");

        Stakeholder saved = stakeholderRepository.save(stakeholder);
        recordHistory(saved, "TREASURER", "APPROVED", "Advance payment recorded: " + payment.getReceiptNo());
        notificationService.createNotification(saved, "Advance Payment Recorded", "Your advance payment has been recorded and sent to the Market Supervisor.");
        auditLogService.log("TREASURER_APPROVED", "Stakeholder", saved.getId(), "Treasurer approved and recorded advance payment");
        syncApplication(saved);
        return saved;
    }

    public Stakeholder assignStallAndCreateContract(
            Long stakeholderId,
            StallAssignmentRequest request
    ) {
        securityService.requireMarketSupervisorOrAdmin();

        Stakeholder stakeholder = getStakeholder(stakeholderId);

        if (!Boolean.TRUE.equals(stakeholder.getTreasurerApproved())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Treasurer approval is required first");
        }

        if (stakeholder.getSelectedStall() == null || stakeholder.getSelectedStall().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Applicant must select an existing vacant stall first");
        }

        Long selectedStallId = stakeholder.getSelectedStall().getId();

        if (request.getStallId() != null && !selectedStallId.equals(request.getStallId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only the applicant-selected stall can be assigned");
        }

        if (occupantRepository.existsByStakeholder_Id(stakeholderId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stakeholder already has an occupant record");
        }

        Stall stall = stallRepository.findById(selectedStallId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stall not found"));

        if (stall.getOccupant() != null || !"AVAILABLE".equalsIgnoreCase(stall.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only available stalls can be assigned");
        }

        LocalDate startDate = request.getStartDate() == null ? LocalDate.now() : request.getStartDate();
        LocalDate endDate = request.getEndDate() == null ? startDate.plusYears(1) : request.getEndDate();

        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }

        Occupant occupant = new Occupant();
        occupant.setStakeholder(stakeholder);
        occupant.setStatus("PENDING");
        occupant.setOccupancyDate(startDate);
        occupant.setAdvanceBalance(stakeholder.getAdvanceBalance() == null ? BigDecimal.ZERO : stakeholder.getAdvanceBalance());
        occupant = occupantRepository.save(occupant);

        stall.setOccupant(occupant);
        stall.setStatus("OCCUPIED");
        stallRepository.save(stall);

        Contract contract = new Contract();
        contract.setOccupant(occupant);
        contract.setStall(stall);
        contract.setContractNo(generateContractNo());
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setMonthlyRent(BigDecimal.valueOf(stall.getMonthlyRent()));
        contract.setBillingFrequency("MONTHLY");
        contract.setTerms(request.getTerms());
        contract.setStatus("PENDING_APPROVAL");
        contract = contractRepository.save(contract);

        occupant.setStall(stall);
        occupant.setContractId(contract.getId());
        occupantRepository.save(occupant);

        stakeholder.setOccupant(occupant);
        stakeholder.setMarketSupervisorApproved(true);
        stakeholder.setMarketApprovalStatus("APPROVED");
        stakeholder.setApplicationStatus("PENDING_BPLO_APPROVAL");
        Stakeholder saved = stakeholderRepository.save(stakeholder);

        recordHistory(saved, "MARKET_SUPERVISOR", "APPROVED", "Assigned stall " + stall.getStallNo());
        notificationService.createNotification(saved, "Stall Assigned", "Your stall and contract have been prepared.");
        auditLogService.log("STALL_ASSIGNED", "Stakeholder", saved.getId(), "Market supervisor assigned stall and created contract");
        syncApplication(saved);
        return saved;
    }

    public Stakeholder approveBplo(Long stakeholderId) {
        securityService.requireBploOrAdmin();
        Stakeholder stakeholder = getStakeholder(stakeholderId);

        if (!Boolean.TRUE.equals(stakeholder.getMarketSupervisorApproved())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Market supervisor approval is required first");
        }

        stakeholder.setBploApproved(true);
        stakeholder.setBploStatus("APPROVED");
        stakeholder.setApplicationStatus("PENDING_ENDORSING_OFFICE_APPROVAL");
        Stakeholder saved = stakeholderRepository.save(stakeholder);

        recordHistory(saved, "BPLO", "APPROVED", "BPLO approval recorded");
        notificationService.createNotification(saved, "BPLO Approved", "Your business permit validation has been approved.");
        auditLogService.log("BPLO_APPROVED", "Stakeholder", saved.getId(), "BPLO approved stakeholder");
        syncApplication(saved);
        return saved;
    }

    public Stakeholder finalEndorse(Long stakeholderId) {
        securityService.requireEndorsingOfficerOrAdmin();
        Stakeholder stakeholder = getStakeholder(stakeholderId);

        if (!Boolean.TRUE.equals(stakeholder.getBploApproved())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BPLO approval is required first");
        }

        stakeholder.setFinalEndorsed(true);
        stakeholder.setEndorsingApproved(true);
        stakeholder.setEndorsementStatus("APPROVED");
        stakeholder.setEndorsingStatus("ENDORSED");
        stakeholder.setFinalStatus("APPROVED");
        stakeholder.setEndorsedBy(securityService.currentUser().getUsername());
        stakeholder.setEndorsedAt(LocalDateTime.now());
        stakeholder.setApplicationStatus("PENDING_BUSINESS_PERMIT_PAYMENT");
        stakeholder.setOnboardingStatus("FOR_APPROVAL");
        Stakeholder saved = stakeholderRepository.save(stakeholder);

        recordHistory(saved, "ENDORSING_OFFICER", "APPROVED", "Final endorsement recorded");
        notificationService.createNotification(saved, "Endorsing Office Approved", "Please proceed to the Treasurer for business permit payment.");
        auditLogService.log("FINAL_ENDORSED", "Stakeholder", saved.getId(), "Final endorsement completed");
        syncApplication(saved);
        return saved;
    }

    public Stakeholder collectApplicantFee(
            Long stakeholderId,
            ApplicantFeeRequest request
    ) {
        securityService.requireTreasurerOrAdmin();

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Applicant fee amount is required");
        }

        Stakeholder stakeholder = getStakeholder(stakeholderId);

        if (!Boolean.TRUE.equals(stakeholder.getFinalEndorsed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endorsing office approval is required before business permit payment");
        }

        Payment payment = new Payment();
        payment.setStakeholder(stakeholder);
        payment.setAmount(request.getAmount());
        payment.setPaymentType(PaymentType.BUSINESS_PERMIT_PAYMENT);
        payment.setReferenceNo(request.getReferenceNo());
        payment.setReceiptNo(generateReceiptNo("APP"));
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        LocalDate applicantFeeDate = LocalDate.now();
        LocalDateTime verificationDate = LocalDateTime.now();

        stakeholder.setApplicantFeePaid(true);
        stakeholder.setTreasurerPaid(true);
        stakeholder.setApplicantFeeAmount(request.getAmount());
        stakeholder.setApplicantFeeDate(applicantFeeDate);
        stakeholder.setApplicationStatus("COMPLETED");
        stakeholder.setOnboardingStatus("APPROVED");
        stakeholder.setFinalStatus("APPROVED");
        stakeholder.setApprovedOn(LocalDate.now());
        stakeholder.setVerified(true);
        stakeholder.setVerificationDate(verificationDate);
        if (stakeholder.getUser() != null) {
            stakeholder.getUser().setRole("STAKEHOLDER");
            stakeholder.getUser().setStatus("ACTIVE");
            userRepository.save(stakeholder.getUser());
        }
        Stakeholder saved = stakeholderRepository.save(stakeholder);
        stakeholderRepository.markApplicantFeePaid(
                saved.getId(),
                request.getAmount(),
                applicantFeeDate,
                verificationDate
        );

        activateBillingIfReady(saved);
        recordHistory(saved, "TREASURER", "BUSINESS_PERMIT_PAID", "Business permit receipt: " + payment.getReceiptNo());
        notificationService.createNotification(
                saved,
                "Business Permit Payment Confirmed",
                "Your business permit payment has been confirmed. Your stakeholder account is now active."
        );
        auditLogService.log("BUSINESS_PERMIT_PAYMENT_RECORDED", "Stakeholder", saved.getId(), "Business permit payment recorded");
        syncApplication(saved);
        return saved;
    }

    public RequirementStatusResponse getRequirementStatus(Long stakeholderId) {
        Stakeholder stakeholder = getStakeholder(stakeholderId);
        securityService.requireStakeholderOwnerOrStaff(stakeholder.getUser().getId());

        List<String> submitted = documentRepository.findByStakeholder_Id(stakeholderId)
                .stream()
                .map(doc -> normalizeDocumentType(doc.getDocumentType()))
                .distinct()
                .toList();

        List<String> missing = REQUIRED_DOCUMENTS.stream()
                .filter(required -> !submitted.contains(required))
                .toList();

        return new RequirementStatusResponse(missing.isEmpty(), REQUIRED_DOCUMENTS, submitted, missing);
    }

    public boolean canAccessDashboard(Stakeholder stakeholder) {
        return stakeholder != null
                && "COMPLETED".equals(stakeholder.getApplicationStatus())
                && Boolean.TRUE.equals(stakeholder.getApplicantFeePaid())
                && getRequirementStatus(stakeholder.getId()).isComplete();
    }

    private void activateBillingIfReady(Stakeholder stakeholder) {
        if (!"COMPLETED".equals(stakeholder.getApplicationStatus())
                || !Boolean.TRUE.equals(stakeholder.getApplicantFeePaid())) {
            return;
        }

        contractRepository.findFirstByOccupant_Stakeholder_IdOrderByCreatedAtDesc(stakeholder.getId())
                .ifPresent(contract -> {
                    if (!"ACTIVE".equalsIgnoreCase(contract.getStatus())) {
                        contract.setStatus("ACTIVE");
                        contractRepository.save(contract);
                    }

                    Occupant occupant = contract.getOccupant();
                    if (occupant != null) {
                        occupant.setStatus("ACTIVE");
                        occupant.setAdvanceBalance(stakeholder.getAdvanceBalance() == null ? BigDecimal.ZERO : stakeholder.getAdvanceBalance());
                        occupantRepository.save(occupant);
                    }

                    billingService.generateInitialBilling(contract);
                });
    }

    private Stakeholder getStakeholder(Long id) {
        return stakeholderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stakeholder not found"));
    }

    private void syncApplication(Stakeholder stakeholder) {
        if (stakeholder.getUser() == null || stakeholder.getUser().getId() == null) {
            return;
        }

        applicationRepository.findByUser_Id(stakeholder.getUser().getId())
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
                    application.setVerifiedApplication(
                            "COMPLETED".equals(stakeholder.getApplicationStatus())
                                    && Boolean.TRUE.equals(stakeholder.getApplicantFeePaid())
                    );
                    application.setApprovedOn(stakeholder.getApprovedOn());
                    applicationRepository.save(application);
                });
    }

    private void recordHistory(
            Stakeholder stakeholder,
            String stage,
            String status,
            String remarks
    ) {
        ApprovalHistory history = new ApprovalHistory();
        history.setStakeholder(stakeholder);
        history.setStage(stage);
        history.setStatus(status);
        history.setRemarks(remarks);
        history.setApprovedBy(securityService.currentUser());
        approvalHistoryRepository.save(history);
    }

    private String generateReceiptNo(String prefix) {
        return "RCPT-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateContractNo() {
        return "CON-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String normalizeDocumentType(String documentType) {
        if (documentType == null) {
            return "";
        }

        String normalized = documentType.trim().toUpperCase().replace(" ", "_").replace("-", "_");

        if ("ID".equals(normalized) || "VALIDID".equals(normalized)) {
            return "VALID_ID";
        }

        if ("BARANGAY".equals(normalized) || "BARANGAY_CLEARANCE".equals(normalized)) {
            return "BARANGAY_CLEARANCE";
        }

        if ("DTI".equals(normalized) || "DTI_PERMIT".equals(normalized)) {
            return "DTI_PERMIT";
        }

        return normalized;
    }
}
