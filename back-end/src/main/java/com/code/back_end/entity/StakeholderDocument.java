package com.code.back_end.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "stakeholder_documents",
        indexes = {

                @Index(
                        name = "idx_document_stakeholder",
                        columnList = "stakeholder_id"
                ),

                @Index(
                        name = "idx_document_type",
                        columnList = "document_type"
                ),

                @Index(
                        name = "idx_document_uploaded_at",
                        columnList = "uploaded_at"
                )
        }
)
public class StakeholderDocument {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    // ======================
    // STAKEHOLDER
    // ======================
    @ManyToOne
    @JoinColumn(name = "stakeholder_id")
    @JsonBackReference
    private Stakeholder stakeholder;

    // ======================
    // DOCUMENT DETAILS
    // ======================
    private String documentType;

    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String filePath;

    private LocalDateTime uploadedAt =
            LocalDateTime.now();

    // ======================
    // GETTERS & SETTERS
    // ======================

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Stakeholder getStakeholder() {
        return stakeholder;
    }

    public void setStakeholder(
            Stakeholder stakeholder
    ) {
        this.stakeholder =
                stakeholder;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(
            String documentType
    ) {
        this.documentType =
                documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(
            String fileName
    ) {
        this.fileName =
                fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(
            String filePath
    ) {
        this.filePath =
                filePath;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(
            LocalDateTime uploadedAt
    ) {
        this.uploadedAt =
                uploadedAt;
    }
}