package com.code.back_end.service;

import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.StakeholderDocument;
import com.code.back_end.exception.BadRequestException;
import com.code.back_end.exception.ResourceNotFoundException;
import com.code.back_end.repository.StakeholderDocumentRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.security.SecurityService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class StakeholderDocumentService {

    private final StakeholderRepository stakeholderRepo;
    private final StakeholderDocumentRepository documentRepo;
    private final FileStorageService fileStorageService;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;

    public StakeholderDocumentService(
            StakeholderRepository stakeholderRepo,
            StakeholderDocumentRepository documentRepo,
            FileStorageService fileStorageService,
            SecurityService securityService,
            AuditLogService auditLogService
    ) {
        this.stakeholderRepo = stakeholderRepo;
        this.documentRepo = documentRepo;
        this.fileStorageService = fileStorageService;
        this.securityService = securityService;
        this.auditLogService = auditLogService;
    }

    public String saveFile(MultipartFile file) throws IOException {
        return fileStorageService.storeFile(file);
    }

    public StakeholderDocument saveDocument(
            Stakeholder stakeholder,
            String documentType,
            MultipartFile file
    ) throws IOException {
        String filePath = saveFile(file);
        String fileName = new File(filePath).getName();

        StakeholderDocument doc = new StakeholderDocument();
        doc.setStakeholder(stakeholder);
        doc.setDocumentType(documentType);
        doc.setFileName(fileName);
        doc.setFilePath(filePath);

        return documentRepo.save(doc);
    }

    public Stakeholder uploadDocument(
            Long userId,
            String type,
            MultipartFile file
    ) throws IOException {
        Stakeholder stakeholder = stakeholderRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Stakeholder not found"));

        securityService.requireStakeholderOwnerOrStaff(stakeholder.getUser().getId());

        String documentType = normalizeDocumentType(type);
        String filePath = saveFile(file);
        String fileName = new File(filePath).getName();

        List<StakeholderDocument> docs = documentRepo.findByStakeholder_Id(stakeholder.getId());
        StakeholderDocument existing = docs.stream()
                .filter(doc -> doc.getDocumentType().equals(documentType))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setFileName(fileName);
            existing.setFilePath(filePath);
            documentRepo.save(existing);
        } else {
            StakeholderDocument newDoc = new StakeholderDocument();
            newDoc.setStakeholder(stakeholder);
            newDoc.setDocumentType(documentType);
            newDoc.setFileName(fileName);
            newDoc.setFilePath(filePath);
            documentRepo.save(newDoc);
        }

        stakeholder.setDocuments(documentRepo.findByStakeholder_Id(stakeholder.getId()));

        auditLogService.log(
                "APPLICATION_DOCUMENT_UPLOADED",
                "Stakeholder",
                stakeholder.getId(),
                "Uploaded document " + documentType
        );

        return stakeholder;
    }

    public List<StakeholderDocument> getDocuments(Long stakeholderId) {
        return documentRepo.findByStakeholder_Id(stakeholderId);
    }

    private String normalizeDocumentType(String type) {
        if (type == null) {
            throw new BadRequestException("Invalid document type");
        }
        switch (type) {
            case "letterFile":
                return "LETTER";
            case "idFile":
            case "validId":
            case "valid-id":
            case "VALID_ID":
                return "ID";
            case "dtiPermit":
            case "dti-permit":
            case "DTI_PERMIT":
                return "DTI_PERMIT";
            case "cedula":
            case "CEDULA":
                return "CEDULA";
            case "barangayClearance":
            case "barangay-clearance":
            case "BARANGAY_CLEARANCE":
                return "BARANGAY_CLEARANCE";
            default:
                throw new BadRequestException("Invalid document type: " + type);
        }
    }
}
