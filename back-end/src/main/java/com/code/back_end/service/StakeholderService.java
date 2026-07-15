package com.code.back_end.service;

import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.StakeholderDocument;
import com.code.back_end.entity.User;
import com.code.back_end.entity.Contract;
import com.code.back_end.repository.StakeholderDocumentRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.repository.BusinessApplicationRepository;
import com.code.back_end.repository.UserRepository;
import com.code.back_end.entity.Occupant;
import com.code.back_end.repository.ContractRepository;
import com.code.back_end.repository.OccupantRepository;
import com.code.back_end.security.SecurityService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StakeholderService {

    private final StakeholderRepository repo;
private final UserRepository userRepo;
private final StakeholderDocumentRepository documentRepo;
private final OccupantRepository occupantRepo;
private final ContractRepository contractRepo;
private final BillingService billingService;
private final SecurityService securityService;
private final AuditLogService auditLogService;
private final BusinessApplicationRepository applicationRepo;
private final NotificationService notificationService;

public StakeholderService(
        StakeholderRepository repo,
        UserRepository userRepo,
        StakeholderDocumentRepository documentRepo,
        OccupantRepository occupantRepo,
        ContractRepository contractRepo,
        BillingService billingService,
        SecurityService securityService,
        AuditLogService auditLogService,
        BusinessApplicationRepository applicationRepo,
        NotificationService notificationService
) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.documentRepo = documentRepo;
    this.occupantRepo = occupantRepo;
    this.contractRepo = contractRepo;
    this.billingService = billingService;
    this.securityService = securityService;
    this.auditLogService = auditLogService;
    this.applicationRepo = applicationRepo;
    this.notificationService = notificationService;
}

    // =========================
    // SAVE FILE HELPER
    // =========================
    private String saveFile(MultipartFile file) throws IOException {

        String uploadDir =
                System.getProperty("user.dir") + "/uploads/";

        File folder = new File(uploadDir);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String originalName = file.getOriginalFilename();

        String cleanName =
                originalName == null
                        ? "file"
                        : originalName.replaceAll(
                                "[^a-zA-Z0-9.]",
                                "_"
                        );

        String fileName =
                System.currentTimeMillis()
                        + "_"
                        + cleanName;

        String filePath = uploadDir + fileName;

        file.transferTo(new File(filePath));

        return filePath;
    }

    // =========================
    // CREATE STAKEHOLDER
    // =========================
    public Stakeholder create(
            Long userId,
            String businessName,
            String businessType,
            String firstName,
            String middleName,
            String lastName,
            String contact,
            String email,
            String address,
            MultipartFile idFile,
            MultipartFile letterFile
    ) throws IOException {

        User user = userRepo.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        securityService.requireSelfUserOrStaff(userId);

        // Prevent duplicate stakeholder for same user
        if (repo.findByUser_Id(userId).isPresent()) {
            throw new RuntimeException(
                    "Stakeholder already exists for this user"
            );
        }

        Stakeholder stakeholder = new Stakeholder();

        stakeholder.setUser(user);
        stakeholder.setBusinessName(businessName);
        stakeholder.setBusinessType(businessType);
        stakeholder.setFirstName(firstName);
        stakeholder.setMiddleName(middleName);
        stakeholder.setLastName(lastName);
        stakeholder.setContact(contact);
        stakeholder.setEmail(email);
        stakeholder.setAddress(address);
        stakeholder.setAppliedOn(LocalDate.now());
        stakeholder.setApplicationStatus("PENDING");
        stakeholder.setOnboardingStatus("PAYMENT_PENDING");
        stakeholder.setMarketApprovalStatus("PENDING");
        stakeholder.setEndorsementStatus("PENDING");
        stakeholder.setBploStatus("PENDING");
        stakeholder.setAdvancePayment(false);
        stakeholder.setAdvancePaymentPaid(false);
        stakeholder.setApplicantFeePaid(false);
        stakeholder.setVerifiedStakeholder(false);

        stakeholder.setTreasurerPaid(false);

        stakeholder.setMarketSupervisorApproved(false);

        stakeholder.setBploApproved(false);

        stakeholder.setEndorsingApproved(false);

        Stakeholder savedStakeholder =
                repo.save(stakeholder);

        // Save documents if provided
        if (idFile != null && !idFile.isEmpty()) {
            saveDocument(
                    savedStakeholder,
                    "ID",
                    idFile
            );
        }

        if (letterFile != null && !letterFile.isEmpty()) {
            saveDocument(
                    savedStakeholder,
                    "LETTER",
                    letterFile
            );
        }

        savedStakeholder.setDocuments(
                documentRepo.findByStakeholder_Id(
                        savedStakeholder.getId()
                )
        );

        updateOverallStatus(savedStakeholder);

        auditLogService.log(
                "APPLICATION_CREATED",
                "Stakeholder",
                savedStakeholder.getId(),
                "Stakeholder application submitted"
        );

        return savedStakeholder;
    }

    // =========================
    // SAVE DOCUMENT
    // =========================
    private void saveDocument(
            Stakeholder stakeholder,
            String documentType,
            MultipartFile file
    ) throws IOException {

        String filePath = saveFile(file);
        String fileName =
                new File(filePath).getName();

        StakeholderDocument doc =
                new StakeholderDocument();

        doc.setStakeholder(stakeholder);
        doc.setDocumentType(documentType);
        doc.setFileName(fileName);
        doc.setFilePath(filePath);

        documentRepo.save(doc);
    }

    // =========================
    // GET BY USER ID
    // IMPORTANT FIX:
    // Returns null instead of throwing 404
    // if user has no stakeholder yet.
    // =========================
    public Stakeholder getByUserId(Long userId) {

        securityService.requireSelfUserOrStaff(userId);

        Optional<Stakeholder> optional =
                repo.findByUser_Id(userId);

        if (optional.isEmpty()) {
            return null;
        }

        Stakeholder stakeholder =
                optional.get();

        stakeholder.setDocuments(
                documentRepo.findByStakeholder_Id(
                        stakeholder.getId()
                )
        );

        return stakeholder;
    }

    // =========================
    // UPLOAD DOCUMENT
    // =========================
    public Stakeholder uploadDocument(
            Long userId,
            String type,
            MultipartFile file
    ) throws IOException {

        Stakeholder stakeholder =
                repo.findByUser_Id(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Stakeholder not found"
                                )
                        );

        securityService.requireStakeholderOwnerOrStaff(
                stakeholder.getUser().getId()
        );

        String documentType;

        switch (type) {
            case "letterFile":
                documentType = "LETTER";
                break;
            case "idFile":
            case "validId":
            case "valid-id":
            case "VALID_ID":
                documentType = "ID";
                break;
            case "dtiPermit":
            case "dti-permit":
            case "DTI_PERMIT":
                documentType = "DTI_PERMIT";
                break;
            case "cedula":
            case "CEDULA":
                documentType = "CEDULA";
                break;
            case "barangayClearance":
            case "barangay-clearance":
            case "BARANGAY_CLEARANCE":
                documentType = "BARANGAY_CLEARANCE";
                break;
            default:
                throw new RuntimeException(
                        "Invalid document type"
                );
        }

        String filePath = saveFile(file);
        String fileName =
                new File(filePath).getName();

        List<StakeholderDocument> docs =
                documentRepo.findByStakeholder_Id(
                        stakeholder.getId()
                );

        StakeholderDocument existing =
                docs.stream()
                        .filter(doc ->
                                doc.getDocumentType()
                                        .equals(documentType)
                        )
                        .findFirst()
                        .orElse(null);

        if (existing != null) {
            existing.setFileName(fileName);
            existing.setFilePath(filePath);
            documentRepo.save(existing);
        } else {
            StakeholderDocument newDoc =
                    new StakeholderDocument();

            newDoc.setStakeholder(stakeholder);
            newDoc.setDocumentType(documentType);
            newDoc.setFileName(fileName);
            newDoc.setFilePath(filePath);

            documentRepo.save(newDoc);
        }

        stakeholder.setDocuments(
                documentRepo.findByStakeholder_Id(
                        stakeholder.getId()
                )
        );

        updateOverallStatus(stakeholder);

        auditLogService.log(
                "APPLICATION_DOCUMENT_UPLOADED",
                "Stakeholder",
                stakeholder.getId(),
                "Uploaded document " + documentType
        );

        return stakeholder;
    }

    // =========================
    // UPDATE OVERALL STATUS
    // =========================
    public void updateOverallStatus(
            Stakeholder stakeholder
    ) {

        List<StakeholderDocument> docs =
                stakeholder.getDocuments();

        if (docs == null) {
            docs = List.of();
        }

        boolean hasLetter =
                docs.stream()
                        .anyMatch(doc ->
                                "LETTER".equals(
                                        doc.getDocumentType()
                                )
                        );

        boolean hasId =
                docs.stream()
                        .anyMatch(doc ->
                                "ID".equals(
                                        doc.getDocumentType()
                                )
                        );

        // Documents are prerequisites but not part
        // of the verifiedTenant auto-check, so we
        // only update verification if docs are present.
        if (hasLetter && hasId) {
            stakeholder.updateVerificationStatus();
        }

        repo.save(stakeholder);

        // =========================
        // AUTO CREATE OCCUPANT
        // =========================
        if (Boolean.TRUE.equals(stakeholder.getVerifiedTenant())) {
            boolean exists =
                    occupantRepo.existsByStakeholder_Id(
                            stakeholder.getId()
                    );
            if (!exists) {
                Occupant occupant = new Occupant();
                occupant.setStakeholder(stakeholder);
                occupantRepo.save(occupant);
            }
        }
    }

    // =========================
    // GET ALL
    // =========================
    public List<Stakeholder> getAll() {
        securityService.requireSupervisorOrAdmin();
        return repo.findAll();
    }

    public List<Stakeholder> getForApproval() {
        securityService.requireSupervisorOrAdmin();
        return repo.findForApproval();
    }

    private Stakeholder findForApproval(Long id) {
        return repo.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Stakeholder not found"
                        )
                );
    }

    private void updateApplicationStatusFromStages(
            Stakeholder stakeholder
    ) {
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

    public Stakeholder refreshOnboardingStatus(
            Stakeholder stakeholder
    ) {
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

        Stakeholder saved = repo.save(stakeholder);
        syncApplicationFromStakeholder(saved);
        return saved;
    }

    public Stakeholder refreshOnboardingStatus(
            Long stakeholderId
    ) {
        Stakeholder stakeholder =
                findForApproval(stakeholderId);

        return refreshOnboardingStatus(stakeholder);
    }

    private void syncApplicationFromStakeholder(
            Stakeholder stakeholder
    ) {
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

    private void activateOccupantAndBilling(
            Stakeholder stakeholder
    ) {
        Occupant occupant =
                occupantRepo.findByStakeholder_IdAndIsArchivedFalse(
                        stakeholder.getId()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Stakeholder has no assigned stall"
                        )
                );

        Contract contract =
                contractRepo.findFirstByOccupant_Stakeholder_IdOrderByCreatedAtDesc(
                        stakeholder.getId()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Stakeholder has no contract"
                        )
                );

        occupant.setStatus("ACTIVE");

        if (occupant.getOccupancyDate() == null) {
            occupant.setOccupancyDate(LocalDate.now());
        }

        occupant.setContractId(contract.getId());
        occupant.setAdvanceBalance(
                stakeholder.getAdvanceBalance() == null
                        ? BigDecimal.ZERO
                        : stakeholder.getAdvanceBalance()
        );
        occupantRepo.save(occupant);

        if (!"ACTIVE".equalsIgnoreCase(contract.getStatus())) {
            contract.setStatus("ACTIVE");
            contractRepo.save(contract);
        }

        billingService.generateInitialBilling(contract);
    }

    // =========================
    // GET BY ID
    // =========================
    public Stakeholder getById(Long id) {
        Stakeholder stakeholder =
                repo.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Stakeholder not found"
                                )
                        );

        stakeholder.setDocuments(
                documentRepo.findByStakeholder_Id(
                        stakeholder.getId()
                )
        );

        securityService.requireStakeholderOwnerOrStaff(
                stakeholder.getUser().getId()
        );

        return stakeholder;
    }

    // =========================
    // MARKET SUPERVISOR APPROVE
    // =========================
    public Stakeholder approveMarketSupervisor(
            Long id
    ) {

        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder =
                findForApproval(id);

        if (!Boolean.TRUE.equals(stakeholder.getAdvancePaymentPaid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Advance payment must be paid before market approval"
            );
        }

        stakeholder.setMarketApprovalStatus("APPROVED");

        updateApplicationStatusFromStages(stakeholder);

        Stakeholder saved =
                refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_SUPERVISOR_APPROVED",
                "Stakeholder",
                saved.getId(),
                "Market supervisor approved stakeholder"
        );

        return saved;
    }

    public Stakeholder rejectMarketSupervisor(
            Long id
    ) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder =
                findForApproval(id);

        stakeholder.setMarketApprovalStatus("REJECTED");
        stakeholder.setEndorsementStatus("PENDING");
        stakeholder.setBploStatus("PENDING");
        updateApplicationStatusFromStages(stakeholder);

        Stakeholder saved =
                refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_SUPERVISOR_REJECTED",
                "Stakeholder",
                saved.getId(),
                "Market supervisor rejected stakeholder"
        );

        return saved;
    }

    // =========================
    // BPLO APPROVE
    // =========================
    public Stakeholder approveBplo(
            Long id
    ) {

        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder =
                findForApproval(id);

        if (!contractRepo.existsByOccupant_Stakeholder_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "BPLO approval requires an existing contract"
            );
        }

        stakeholder.setBploStatus("APPROVED");
        updateApplicationStatusFromStages(stakeholder);

        Stakeholder saved =
                refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_BPLO_APPROVED",
                "Stakeholder",
                saved.getId(),
                "BPLO approved stakeholder"
        );

        return saved;
    }

    public Stakeholder rejectBplo(
            Long id
    ) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder =
                findForApproval(id);

        if (!contractRepo.existsByOccupant_Stakeholder_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "BPLO rejection requires an existing contract"
            );
        }

        stakeholder.setBploStatus("REJECTED");
        updateApplicationStatusFromStages(stakeholder);

        Stakeholder saved =
                refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_BPLO_REJECTED",
                "Stakeholder",
                saved.getId(),
                "BPLO rejected stakeholder"
        );

        return saved;
    }

    // =========================
    // ENDORSING APPROVE
    // =========================
    public Stakeholder approveEndorsing(
            Long id
    ) {

        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder =
                findForApproval(id);

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

        Stakeholder saved =
                refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_ENDORSING_APPROVED",
                "Stakeholder",
                saved.getId(),
                "Endorsing office approved stakeholder"
        );

        return saved;
    }

    public Stakeholder rejectEndorsing(
            Long id,
            String remarks
    ) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder =
                findForApproval(id);

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

        Stakeholder saved =
                refreshOnboardingStatus(stakeholder);

        auditLogService.log(
                "STAKEHOLDER_ENDORSING_REJECTED",
                "Stakeholder",
                saved.getId(),
                "Endorsing office rejected stakeholder"
        );

        return saved;
    }

    public Stakeholder rejectEndorsing(
            Long id
    ) {
        return rejectEndorsing(id, null);
    }

    public Stakeholder payApplicantFee(
            Long id,
            BigDecimal amount
    ) {
        securityService.requireSupervisorOrAdmin();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Applicant fee amount must be greater than zero"
            );
        }

        Stakeholder stakeholder =
                findForApproval(id);

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

        Stakeholder saved =
                refreshOnboardingStatus(stakeholder);

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

    // =========================
    // FINAL APPROVE
    // =========================
    public Stakeholder approve(Long id) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder =
                getById(id);

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

        Stakeholder saved =
                refreshOnboardingStatus(stakeholder);

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

    // =========================
    // REJECT
    // =========================
    public Stakeholder reject(Long id) {
        return reject(id, null);
    }

    public Stakeholder reject(Long id, String remarks) {
        securityService.requireSupervisorOrAdmin();

        Stakeholder stakeholder =
                getById(id);

        stakeholder.setApplicationStatus(
                "REJECTED"
        );
        stakeholder.setOnboardingStatus("REJECTED");
        stakeholder.setMarketApprovalStatus("REJECTED");
        stakeholder.setEndorsementStatus("PENDING");
        stakeholder.setBploStatus("PENDING");
        stakeholder.setRemarks(remarks);
        stakeholder.setNotes(remarks);

        Stakeholder saved =
                refreshOnboardingStatus(stakeholder);

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

    // =========================
    // DELETE
    // =========================
    public String delete(Long id) {
        securityService.requireAdmin();

        Stakeholder stakeholder =
                 getById(id);

        repo.delete(stakeholder);

        return "Stakeholder deleted successfully";
    }
}
