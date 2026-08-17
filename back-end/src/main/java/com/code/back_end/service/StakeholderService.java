package com.code.back_end.service;

import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.User;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.repository.UserRepository;
import com.code.back_end.security.SecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class StakeholderService {

    private final StakeholderRepository repo;
    private final UserRepository userRepo;
    private final StakeholderDocumentService documentService;
    private final ApplicantApprovalService applicantApprovalService;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;

    public StakeholderService(
            StakeholderRepository repo,
            UserRepository userRepo,
            StakeholderDocumentService documentService,
            ApplicantApprovalService applicantApprovalService,
            SecurityService securityService,
            AuditLogService auditLogService
    ) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.documentService = documentService;
        this.applicantApprovalService = applicantApprovalService;
        this.securityService = securityService;
        this.auditLogService = auditLogService;
    }

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
                .orElseThrow(() -> new RuntimeException("User not found"));

        securityService.requireSelfUserOrStaff(userId);

        if (repo.findByUser_Id(userId).isPresent()) {
            throw new RuntimeException("Stakeholder already exists for this user");
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

        Stakeholder savedStakeholder = repo.save(stakeholder);

        if (idFile != null && !idFile.isEmpty()) {
            documentService.saveDocument(savedStakeholder, "ID", idFile);
        }

        if (letterFile != null && !letterFile.isEmpty()) {
            documentService.saveDocument(savedStakeholder, "LETTER", letterFile);
        }

        savedStakeholder.setDocuments(documentService.getDocuments(savedStakeholder.getId()));
        applicantApprovalService.updateOverallStatus(savedStakeholder);

        auditLogService.log(
                "APPLICATION_CREATED",
                "Stakeholder",
                savedStakeholder.getId(),
                "Stakeholder application submitted"
        );

        return savedStakeholder;
    }

    public Stakeholder getByUserId(Long userId) {
        securityService.requireSelfUserOrStaff(userId);

        Optional<Stakeholder> optional = repo.findByUser_Id(userId);
        if (optional.isEmpty()) {
            return null;
        }

        Stakeholder stakeholder = optional.get();
        stakeholder.setDocuments(documentService.getDocuments(stakeholder.getId()));
        return stakeholder;
    }

    public Stakeholder getById(Long id) {
        Stakeholder stakeholder = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stakeholder not found"));

        stakeholder.setDocuments(documentService.getDocuments(stakeholder.getId()));
        securityService.requireStakeholderOwnerOrStaff(stakeholder.getUser().getId());
        return stakeholder;
    }

    public List<Stakeholder> getAll() {
        securityService.requireSupervisorOrAdmin();
        return repo.findAll();
    }

    public List<Stakeholder> getForApproval() {
        securityService.requireSupervisorOrAdmin();
        return repo.findForApproval();
    }

    public Stakeholder uploadDocument(Long userId, String type, MultipartFile file) throws IOException {
        Stakeholder stakeholder = documentService.uploadDocument(userId, type, file);
        applicantApprovalService.updateOverallStatus(stakeholder);
        return stakeholder;
    }

    public void updateOverallStatus(Stakeholder stakeholder) {
        applicantApprovalService.updateOverallStatus(stakeholder);
    }

    public Stakeholder refreshOnboardingStatus(Stakeholder stakeholder) {
        return applicantApprovalService.refreshOnboardingStatus(stakeholder);
    }

    public Stakeholder refreshOnboardingStatus(Long stakeholderId) {
        return applicantApprovalService.refreshOnboardingStatus(stakeholderId);
    }

    public Stakeholder approveMarketSupervisor(Long id) {
        return applicantApprovalService.approveMarketSupervisor(id);
    }

    public Stakeholder rejectMarketSupervisor(Long id) {
        return applicantApprovalService.rejectMarketSupervisor(id);
    }

    public Stakeholder approveBplo(Long id) {
        return applicantApprovalService.approveBplo(id);
    }

    public Stakeholder rejectBplo(Long id) {
        return applicantApprovalService.rejectBplo(id);
    }

    public Stakeholder approveEndorsing(Long id) {
        return applicantApprovalService.approveEndorsing(id);
    }

    public Stakeholder rejectEndorsing(Long id, String remarks) {
        return applicantApprovalService.rejectEndorsing(id, remarks);
    }

    public Stakeholder rejectEndorsing(Long id) {
        return applicantApprovalService.rejectEndorsing(id);
    }

    public Stakeholder payApplicantFee(Long id, BigDecimal amount) {
        return applicantApprovalService.payApplicantFee(id, amount);
    }

    public Stakeholder approve(Long id) {
        return applicantApprovalService.approve(id);
    }

    public Stakeholder reject(Long id) {
        return applicantApprovalService.reject(id);
    }

    public Stakeholder reject(Long id, String remarks) {
        return applicantApprovalService.reject(id, remarks);
    }

    public String delete(Long id) {
        securityService.requireAdmin();
        Stakeholder stakeholder = getById(id);
        repo.delete(stakeholder);
        return "Stakeholder deleted successfully";
    }
}
