package com.code.back_end.service;

import com.code.back_end.entity.BusinessApplication;
import com.code.back_end.entity.Stall;
import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.StakeholderDocument;
import com.code.back_end.entity.User;
import com.code.back_end.repository.BusinessApplicationRepository;
import com.code.back_end.repository.StallRepository;
import com.code.back_end.repository.StakeholderDocumentRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.repository.UserRepository;
import com.code.back_end.security.SecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public class ApplicationService {

    private final BusinessApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final StallRepository stallRepository;
    private final StakeholderRepository stakeholderRepository;
    private final StakeholderDocumentRepository stakeholderDocumentRepository;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public ApplicationService(
            BusinessApplicationRepository applicationRepository,
            UserRepository userRepository,
            StallRepository stallRepository,
            StakeholderRepository stakeholderRepository,
            StakeholderDocumentRepository stakeholderDocumentRepository,
            SecurityService securityService,
            AuditLogService auditLogService,
            NotificationService notificationService
    ) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.stallRepository = stallRepository;
        this.stakeholderRepository = stakeholderRepository;
        this.stakeholderDocumentRepository = stakeholderDocumentRepository;
        this.securityService = securityService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public BusinessApplication create(
            Long userId,
            String businessName,
            String businessType,
            String firstName,
            String middleName,
            String lastName,
            String contact,
            String email,
            String address,
            Long selectedStallId,
            MultipartFile idFile,
            MultipartFile letterFile
    ) throws IOException {
        securityService.requireSelfUserOrStaff(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        if (applicationRepository.existsByUser_Id(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Business application already exists for this user"
            );
        }

        Stall selectedStall = stallRepository.findById(selectedStallId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected stall not found"));

        if (selectedStall.getOccupant() != null || !"AVAILABLE".equalsIgnoreCase(selectedStall.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only existing vacant stalls can be selected");
        }

        BusinessApplication application = new BusinessApplication();
        application.setUser(user);
        application.setBusinessName(businessName);
        application.setBusinessType(businessType);
        application.setFirstName(firstName);
        application.setMiddleName(middleName);
        application.setLastName(lastName);
        application.setContact(contact);
        application.setEmail(email);
        application.setAddress(address);
        application.setSelectedStall(selectedStall);
        application.setAppliedOn(LocalDate.now());
        application.setApplicationStatus("PENDING_TREASURER_APPROVAL");
        application.setOnboardingStatus("FOR_APPROVAL");

        if (idFile != null && !idFile.isEmpty()) {
            String path = saveFile(idFile);
            application.setIdFilePath(path);
            application.setIdFileName(new File(path).getName());
        }

        if (letterFile != null && !letterFile.isEmpty()) {
            String path = saveFile(letterFile);
            application.setLetterFilePath(path);
            application.setLetterFileName(new File(path).getName());
        }

        BusinessApplication saved = applicationRepository.save(application);
        Stakeholder stakeholder = createStakeholderForSubmittedApplication(saved);
        saveApplicationDocument(stakeholder, "VALID_ID", saved.getIdFileName(), saved.getIdFilePath());
        saveApplicationDocument(stakeholder, "LETTER", saved.getLetterFileName(), saved.getLetterFilePath());

        auditLogService.log(
                "APPLICATION_CREATED",
                "BusinessApplication",
                saved.getId(),
                "Business application submitted"
        );

        return saved;
    }

    public List<BusinessApplication> getAll() {
        securityService.requireSupervisorOrAdmin();
        return applicationRepository.findAll();
    }

    public BusinessApplication getByUserId(Long userId) {
        securityService.requireSelfUserOrStaff(userId);

        return applicationRepository.findByUser_Id(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Business application not found"
                        )
                );
    }

    public BusinessApplication getById(Long id) {
        BusinessApplication application =
                applicationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Business application not found"
                                )
                        );

        securityService.requireSelfUserOrStaff(application.getUser().getId());
        return application;
    }

    public BusinessApplication approve(Long id) {
        securityService.requireSupervisorOrAdmin();

        BusinessApplication application = findForStaff(id);
        application.setApplicationStatus("APPROVED");
        application.setVerifiedApplication(application.getApplicantFeePaid());
        application.setApprovedOn(LocalDate.now());
        application.setOnboardingStatus("APPROVED");

        BusinessApplication saved = applicationRepository.save(application);
        approveStakeholderAfterApplicationApproval(saved);

        auditLogService.log(
                "APPLICATION_APPROVED",
                "BusinessApplication",
                saved.getId(),
                "Business application approved"
        );

        return saved;
    }

    public BusinessApplication reject(Long id) {
        securityService.requireSupervisorOrAdmin();

        BusinessApplication application = findForStaff(id);
        application.setApplicationStatus("REJECTED");
        application.setVerifiedApplication(false);
        application.setOnboardingStatus("REJECTED");

        BusinessApplication saved = applicationRepository.save(application);

        auditLogService.log(
                "APPLICATION_REJECTED",
                "BusinessApplication",
                saved.getId(),
                "Business application rejected"
        );

        return saved;
    }

    public BusinessApplication endorse(Long id) {
        securityService.requireEndorsingOfficerOrAdmin();

        BusinessApplication application = findForStaff(id);
        ensureBploApproved(application);
        ensureNotRejected(application);

        if ("ENDORSED".equals(application.getEndorsingStatus()) || "APPROVED".equals(application.getEndorsementStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is already endorsed");
        }

        application.setEndorsingStatus("ENDORSED");
        application.setEndorsementStatus("APPROVED");
        application.setEndorsedBy(securityService.currentUser().getUsername());
        application.setEndorsedAt(java.time.LocalDateTime.now());
        application.setRemarks(null);
        application.setEndorsementRemarks(null);
        application.setFinalStatus("APPROVED");
        application.setApplicationStatus("PENDING_BUSINESS_PERMIT_PAYMENT");
        application.setOnboardingStatus("FOR_APPROVAL");

        BusinessApplication saved = applicationRepository.save(application);
        syncStakeholderFromApplication(saved);
        notifyStakeholder(saved, "Endorsing Office Approved", "Please proceed to the Treasurer for business permit payment.");
        auditLogService.log("APPLICATION_ENDORSED", "BusinessApplication", saved.getId(), "Endorsing office endorsed application");

        return saved;
    }

    public BusinessApplication rejectEndorsement(Long id, String remarks) {
        securityService.requireEndorsingOfficerOrAdmin();

        BusinessApplication application = findForStaff(id);
        ensureBploApproved(application);
        ensureNoFinalDecision(application);

        if ("REJECTED".equals(application.getEndorsingStatus()) || "REJECTED".equals(application.getEndorsementStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application endorsement is already rejected");
        }

        application.setEndorsingStatus("REJECTED");
        application.setEndorsementStatus("REJECTED");
        application.setFinalStatus("REJECTED");
        application.setApplicationStatus("REJECTED");
        application.setOnboardingStatus("REJECTED");
        application.setVerifiedApplication(false);
        application.setEndorsedBy(securityService.currentUser().getUsername());
        application.setEndorsedAt(java.time.LocalDateTime.now());
        application.setRemarks(remarks);
        application.setEndorsementRemarks(remarks);

        BusinessApplication saved = applicationRepository.save(application);
        syncStakeholderFromApplication(saved);
        notifyStakeholder(saved, "Endorsement Rejected", "Your application was rejected during endorsing office review.");
        auditLogService.log("APPLICATION_ENDORSEMENT_REJECTED", "BusinessApplication", saved.getId(), "Endorsing office rejected application");

        return saved;
    }

    public BusinessApplication approveByBplo(Long id) {
        securityService.requireBploOrAdmin();

        BusinessApplication application = findForStaff(id);
        ensureMarketApproved(application);
        ensureNoFinalDecision(application);

        if ("APPROVED".equals(application.getBploStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "BPLO approval already recorded");
        }

        application.setBploStatus("APPROVED");
        application.setFinalStatus("PENDING");
        application.setApplicationStatus("PENDING_ENDORSING_OFFICE_APPROVAL");
        application.setOnboardingStatus("FOR_APPROVAL");
        application.setVerifiedApplication(false);
        application.setBploApprovedBy(securityService.currentUser().getUsername());
        application.setApprovalDate(java.time.LocalDateTime.now());
        application.setApprovedOn(LocalDate.now());
        application.setRemarks(null);

        BusinessApplication saved = applicationRepository.save(application);
        syncStakeholderFromApplication(saved);
        notifyStakeholder(saved, "BPLO Approved", "Your business permit requirements were approved and sent to the Endorsing Office.");
        auditLogService.log("APPLICATION_BPLO_APPROVED", "BusinessApplication", saved.getId(), "BPLO office approved application");

        return saved;
    }

    public BusinessApplication rejectByBplo(Long id, String remarks) {
        securityService.requireBploOrAdmin();

        BusinessApplication application = findForStaff(id);
        ensureMarketApproved(application);
        ensureNoFinalDecision(application);

        application.setBploStatus("REJECTED");
        application.setFinalStatus("REJECTED");
        application.setApplicationStatus("REJECTED");
        application.setOnboardingStatus("REJECTED");
        application.setVerifiedApplication(false);
        application.setBploApprovedBy(securityService.currentUser().getUsername());
        application.setApprovalDate(java.time.LocalDateTime.now());
        application.setRemarks(remarks);

        BusinessApplication saved = applicationRepository.save(application);
        syncStakeholderFromApplication(saved);
        notifyStakeholder(saved, "BPLO Rejected", "Your application was rejected during final BPLO review.");
        auditLogService.log("APPLICATION_BPLO_REJECTED", "BusinessApplication", saved.getId(), "BPLO office rejected application");

        return saved;
    }

    private BusinessApplication findForStaff(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Business application not found"
                        )
                );
    }

    private void ensureMarketApproved(BusinessApplication application) {
        if (!"APPROVED".equals(application.getMarketApprovalStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Market supervisor approval is required first");
        }
    }

    private void ensureEndorsed(BusinessApplication application) {
        if (!"ENDORSED".equals(application.getEndorsingStatus()) && !"APPROVED".equals(application.getEndorsementStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endorsing office endorsement is required first");
        }
    }

    private void ensureBploApproved(BusinessApplication application) {
        if (!"APPROVED".equals(application.getBploStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BPLO approval is required first");
        }
    }

    private void ensureNotRejected(BusinessApplication application) {
        if (
                "REJECTED".equals(application.getMarketApprovalStatus())
                        || "REJECTED".equals(application.getEndorsingStatus())
                        || "REJECTED".equals(application.getEndorsementStatus())
                        || "REJECTED".equals(application.getBploStatus())
                        || "REJECTED".equals(application.getFinalStatus())
        ) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rejected applications cannot be approved");
        }
    }

    private void ensureNoFinalDecision(BusinessApplication application) {
        if ("APPROVED".equals(application.getFinalStatus()) || "REJECTED".equals(application.getFinalStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application already has a final decision");
        }
    }

    private void notifyStakeholder(BusinessApplication application, String title, String message) {
        stakeholderRepository.findByUser_Id(application.getUser().getId())
                .ifPresent(stakeholder -> notificationService.createNotification(stakeholder, title, message));
    }

    private void syncStakeholderFromApplication(BusinessApplication application) {
        stakeholderRepository.findByUser_Id(application.getUser().getId())
                .ifPresent(stakeholder -> {
                    stakeholder.setApplicationStatus(application.getApplicationStatus());
                    stakeholder.setOnboardingStatus(application.getOnboardingStatus());
                    stakeholder.setMarketApprovalStatus(application.getMarketApprovalStatus());
                    stakeholder.setEndorsingStatus(application.getEndorsingStatus());
                    stakeholder.setEndorsementStatus(application.getEndorsementStatus());
                    stakeholder.setEndorsementRemarks(application.getEndorsementRemarks());
                    stakeholder.setEndorsedAt(application.getEndorsedAt());
                    stakeholder.setBploStatus(application.getBploStatus());
                    stakeholder.setFinalStatus(application.getFinalStatus());
                    stakeholder.setEndorsedBy(application.getEndorsedBy());
                    stakeholder.setBploApprovedBy(application.getBploApprovedBy());
                    stakeholder.setApprovalDate(application.getApprovalDate());
                    stakeholder.setRemarks(application.getRemarks());
                    stakeholder.setSelectedStall(application.getSelectedStall());
                    stakeholder.setVerifiedStakeholder(application.getApplicantFeePaid());
                    stakeholder.setVerifiedTenant(application.getApplicantFeePaid());
                    stakeholder.setApprovedOn(application.getApprovedOn());
                    stakeholderRepository.save(stakeholder);
                });
    }

    private Stakeholder createStakeholderForSubmittedApplication(
            BusinessApplication application
    ) {
        Long userId = application.getUser().getId();

        return stakeholderRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    Stakeholder stakeholder = new Stakeholder();
                    stakeholder.setUser(application.getUser());
                    stakeholder.setBusinessName(application.getBusinessName());
                    stakeholder.setBusinessType(application.getBusinessType());
                    stakeholder.setFirstName(application.getFirstName());
                    stakeholder.setMiddleName(application.getMiddleName());
                    stakeholder.setLastName(application.getLastName());
                    stakeholder.setContact(application.getContact());
                    stakeholder.setEmail(application.getEmail());
                    stakeholder.setAddress(application.getAddress());
                    stakeholder.setSelectedStall(application.getSelectedStall());
                    stakeholder.setAppliedOn(application.getAppliedOn());
                    stakeholder.setApplicationStatus("PENDING_TREASURER_APPROVAL");
                    stakeholder.setOnboardingStatus("FOR_APPROVAL");
                    stakeholder.setAdvancePaymentPaid(false);
                    stakeholder.setAdvancePaymentCompleted(false);
                    stakeholder.setApplicantFeePaid(false);
                    stakeholder.setVerifiedStakeholder(false);
                    stakeholder.setVerifiedTenant(false);
                    stakeholder.setTreasurerPaid(false);
                    stakeholder.setMarketApprovalStatus("PENDING");
                    stakeholder.setBploStatus("PENDING");
                    stakeholder.setEndorsementStatus("PENDING");
                    stakeholder.setEndorsingStatus("PENDING");
                    stakeholder.setFinalStatus("PENDING");
                    stakeholder.setMarketSupervisorApproved(false);
                    stakeholder.setBploApproved(false);
                    stakeholder.setEndorsingApproved(false);

                    return stakeholderRepository.save(stakeholder);
                });
    }

    private void approveStakeholderAfterApplicationApproval(
            BusinessApplication application
    ) {
        Stakeholder stakeholder =
                createStakeholderForSubmittedApplication(application);

        stakeholder.setApprovedOn(LocalDate.now());
        stakeholder.setApplicationStatus("APPROVED");
        stakeholder.setOnboardingStatus("APPROVED");
        stakeholder.setAdvancePaymentPaid(application.getAdvancePaymentPaid());
        stakeholder.setAdvancePaymentCompleted(application.getAdvancePaymentCompleted());
        stakeholder.setApplicantFeePaid(application.getApplicantFeePaid());
        stakeholder.setVerifiedStakeholder(application.getApplicantFeePaid());
        stakeholder.setVerifiedTenant(application.getApplicantFeePaid());
        stakeholder.setMarketApprovalStatus("APPROVED");
        stakeholder.setBploStatus("APPROVED");
        stakeholder.setEndorsementStatus("APPROVED");
        stakeholder.setEndorsingStatus("ENDORSED");
        stakeholder.setFinalStatus("APPROVED");

        stakeholderRepository.save(stakeholder);
    }

    private String saveFile(MultipartFile file) throws IOException {
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File folder = new File(uploadDir);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String originalName = file.getOriginalFilename();
        String cleanName =
                originalName == null
                        ? "file"
                        : originalName.replaceAll("[^a-zA-Z0-9.]", "_");
        String fileName = System.currentTimeMillis() + "_" + cleanName;
        String filePath = uploadDir + fileName;

        file.transferTo(new File(filePath));
        return filePath;
    }

    private void saveApplicationDocument(
            Stakeholder stakeholder,
            String documentType,
            String fileName,
            String filePath
    ) {
        if (fileName == null || filePath == null) {
            return;
        }

        boolean exists = stakeholderDocumentRepository
                .findByStakeholder_Id(stakeholder.getId())
                .stream()
                .anyMatch(doc -> documentType.equals(doc.getDocumentType()));

        if (exists) {
            return;
        }

        StakeholderDocument document = new StakeholderDocument();
        document.setStakeholder(stakeholder);
        document.setDocumentType(documentType);
        document.setFileName(fileName);
        document.setFilePath(filePath);
        stakeholderDocumentRepository.save(document);
    }
}
