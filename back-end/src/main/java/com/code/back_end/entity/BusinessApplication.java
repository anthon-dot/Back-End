package com.code.back_end.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "business_applications",
        indexes = {
                @Index(
                        name = "idx_business_application_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_business_application_status",
                        columnList = "application_status"
                ),
                @Index(
                        name = "idx_business_application_applied_on",
                        columnList = "applied_on"
                )
        }
)
public class BusinessApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotBlank(message = "Business name is required")
    @Size(max = 120)
    private String businessName;

    @NotBlank(message = "Business type is required")
    @Size(max = 80)
    private String businessType;

    @NotBlank(message = "First name is required")
    @Size(max = 80)
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 80)
    private String lastName;

    @NotBlank(message = "Contact is required")
    @Size(max = 40)
    private String contact;

    @Email(message = "Email must be valid")
    @Size(max = 120)
    private String email;

    @Column(columnDefinition = "TEXT")
    @NotBlank(message = "Address is required")
    private String address;

    @ManyToOne
    @JoinColumn(name = "selected_stall_id")
    private Stall selectedStall;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'PENDING'")
    private String applicationStatus = "PENDING";

    @Column(nullable = false, columnDefinition = "varchar(50) default 'NEW'")
    private String onboardingStatus = "NEW";

    @Column(nullable = false)
    private Boolean applicationFormPaid = false;

    @Column(nullable = false)
    private Boolean advancePaymentCompleted = false;

    @Column(nullable = false)
    private BigDecimal advancePaymentAmount = BigDecimal.ZERO;

    private LocalDate advancePaymentDate;

    @Column(nullable = false)
    private BigDecimal advanceBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalAdvanceAmount = BigDecimal.ZERO;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'PENDING'")
    private String marketApprovalStatus = "PENDING";

    @Column(nullable = false, columnDefinition = "varchar(255) default 'PENDING'")
    private String endorsementStatus = "PENDING";

    @Column(nullable = false, columnDefinition = "varchar(20) default 'PENDING'")
    private String endorsingStatus = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String endorsementRemarks;

    private LocalDateTime endorsedAt;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'PENDING'")
    private String bploStatus = "PENDING";

    @Column(nullable = false, columnDefinition = "varchar(20) default 'PENDING'")
    private String finalStatus = "PENDING";

    private String endorsedBy;

    private String bploApprovedBy;

    private LocalDateTime approvalDate;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false)
    private Boolean marketSupervisorApproved = false;

    @Column(nullable = false)
    private Boolean bploApproved = false;

    @Column(nullable = false)
    private Boolean endorsingApproved = false;

    @Column(nullable = false)
    private Boolean applicantFeePaid = false;

    private BigDecimal applicantFeeAmount = BigDecimal.ZERO;

    private LocalDate applicantFeeDate;

    @Column(nullable = false)
    private Boolean verifiedApplication = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String idFileName;

    private String idFilePath;

    private String letterFileName;

    private String letterFilePath;

    private LocalDate appliedOn = LocalDate.now();

    private LocalDate approvedOn;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    private void normalize() {
        this.applicationStatus = normalizeStatus(this.applicationStatus);
        this.marketApprovalStatus = normalizeStatus(this.marketApprovalStatus);
        this.endorsementStatus = normalizeStatus(this.endorsementStatus);
        this.endorsingStatus = normalizeEndorsingStatus(this.endorsingStatus);
        this.bploStatus = normalizeStatus(this.bploStatus);
        this.finalStatus = normalizeStatus(this.finalStatus);
        this.marketSupervisorApproved = isApproved(this.marketApprovalStatus);
        this.endorsingApproved =
                isApproved(this.endorsementStatus) || "ENDORSED".equals(this.endorsingStatus);
        this.bploApproved = isApproved(this.bploStatus);
        this.onboardingStatus = normalizeOnboardingStatus(this.onboardingStatus);
        this.advancePaymentCompleted = Boolean.TRUE.equals(this.advancePaymentCompleted);
        this.applicantFeePaid = Boolean.TRUE.equals(this.applicantFeePaid);
        this.verifiedApplication = Boolean.TRUE.equals(this.applicantFeePaid);
        this.updatedAt = LocalDateTime.now();
    }

    public void refreshOverallStatus() {
        if (
                "REJECTED".equals(marketApprovalStatus)
                        || "REJECTED".equals(endorsementStatus)
                        || "REJECTED".equals(bploStatus)
        ) {
            applicationStatus = "REJECTED";
            verifiedApplication = false;
            return;
        }

        boolean approved =
                Boolean.TRUE.equals(advancePaymentCompleted)
                        && "APPROVED".equals(marketApprovalStatus)
                        && "APPROVED".equals(bploStatus)
                        && "APPROVED".equals(endorsementStatus)
                        && Boolean.TRUE.equals(applicantFeePaid);

        verifiedApplication = approved;
        applicationStatus = approved ? "APPROVED" : "PENDING";

        if (approved && approvedOn == null) {
            approvedOn = LocalDate.now();
        }
    }

    public String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }

        String normalized = status.trim().toUpperCase();

        if (
                "PENDING".equals(normalized)
                        || "PENDING_TREASURER_APPROVAL".equals(normalized)
                        || "PENDING_MARKET_SUPERVISOR_APPROVAL".equals(normalized)
                        || "PENDING_BPLO_APPROVAL".equals(normalized)
                        || "PENDING_ENDORSING_OFFICE_APPROVAL".equals(normalized)
                        || "PENDING_BUSINESS_PERMIT_PAYMENT".equals(normalized)
                        || "COMPLETED".equals(normalized)
                        || "APPROVED".equals(normalized)
                        || "FULLY_APPROVED".equals(normalized)
                        || "REJECTED".equals(normalized)
        ) {
            return normalized;
        }

        return "PENDING";
    }

    private String normalizeEndorsingStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }

        String normalized = status.trim().toUpperCase();

        if (
                "PENDING".equals(normalized)
                        || "ENDORSED".equals(normalized)
                        || "APPROVED".equals(normalized)
                        || "REJECTED".equals(normalized)
        ) {
            return normalized;
        }

        return "PENDING";
    }

    private boolean isApproved(String status) {
        return "APPROVED".equals(normalizeStatus(status));
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Stall getSelectedStall() {
        return selectedStall;
    }

    public void setSelectedStall(Stall selectedStall) {
        this.selectedStall = selectedStall;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(String applicationStatus) {
        this.applicationStatus = normalizeStatus(applicationStatus);
    }

    public String getOnboardingStatus() {
        return onboardingStatus;
    }

    public void setOnboardingStatus(String onboardingStatus) {
        this.onboardingStatus =
                normalizeOnboardingStatus(onboardingStatus);
    }

    public Boolean getApplicationFormPaid() {
        return applicationFormPaid;
    }

    public void setApplicationFormPaid(Boolean applicationFormPaid) {
        this.applicationFormPaid = Boolean.TRUE.equals(applicationFormPaid);
    }

    public Boolean getAdvancePaymentCompleted() {
        return advancePaymentCompleted;
    }

    public void setAdvancePaymentCompleted(Boolean advancePaymentCompleted) {
        this.advancePaymentCompleted = Boolean.TRUE.equals(advancePaymentCompleted);
    }

    public Boolean getAdvancePaymentPaid() {
        return advancePaymentCompleted;
    }

    public void setAdvancePaymentPaid(Boolean advancePaymentPaid) {
        this.advancePaymentCompleted = Boolean.TRUE.equals(advancePaymentPaid);
    }

    public BigDecimal getAdvancePaymentAmount() {
        return advancePaymentAmount;
    }

    public void setAdvancePaymentAmount(BigDecimal advancePaymentAmount) {
        this.advancePaymentAmount =
                advancePaymentAmount == null ? BigDecimal.ZERO : advancePaymentAmount;
    }

    public LocalDate getAdvancePaymentDate() {
        return advancePaymentDate;
    }

    public void setAdvancePaymentDate(LocalDate advancePaymentDate) {
        this.advancePaymentDate = advancePaymentDate;
    }

    public BigDecimal getAdvanceBalance() {
        return advanceBalance;
    }

    public void setAdvanceBalance(BigDecimal advanceBalance) {
        this.advanceBalance = advanceBalance == null ? BigDecimal.ZERO : advanceBalance;
    }

    public BigDecimal getTotalAdvanceAmount() {
        return totalAdvanceAmount;
    }

    public void setTotalAdvanceAmount(BigDecimal totalAdvanceAmount) {
        this.totalAdvanceAmount =
                totalAdvanceAmount == null ? BigDecimal.ZERO : totalAdvanceAmount;
    }

    public String getMarketApprovalStatus() {
        return marketApprovalStatus;
    }

    public void setMarketApprovalStatus(String marketApprovalStatus) {
        this.marketApprovalStatus = normalizeStatus(marketApprovalStatus);
        this.marketSupervisorApproved = isApproved(this.marketApprovalStatus);
    }

    public String getEndorsementStatus() {
        return endorsementStatus;
    }

    public void setEndorsementStatus(String endorsementStatus) {
        this.endorsementStatus = normalizeStatus(endorsementStatus);
        this.endorsingApproved = isApproved(this.endorsementStatus);
    }

    public String getEndorsingStatus() {
        return endorsingStatus;
    }

    public void setEndorsingStatus(String endorsingStatus) {
        this.endorsingStatus = normalizeEndorsingStatus(endorsingStatus);
        this.endorsingApproved =
                "ENDORSED".equals(this.endorsingStatus) || isApproved(this.endorsementStatus);
    }

    public String getEndorsementRemarks() {
        return endorsementRemarks;
    }

    public void setEndorsementRemarks(String endorsementRemarks) {
        this.endorsementRemarks = endorsementRemarks;
    }

    public LocalDateTime getEndorsedAt() {
        return endorsedAt;
    }

    public void setEndorsedAt(LocalDateTime endorsedAt) {
        this.endorsedAt = endorsedAt;
    }

    public String getBploStatus() {
        return bploStatus;
    }

    public void setBploStatus(String bploStatus) {
        this.bploStatus = normalizeStatus(bploStatus);
        this.bploApproved = isApproved(this.bploStatus);
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = normalizeStatus(finalStatus);
    }

    public String getEndorsedBy() {
        return endorsedBy;
    }

    public void setEndorsedBy(String endorsedBy) {
        this.endorsedBy = endorsedBy;
    }

    public String getBploApprovedBy() {
        return bploApprovedBy;
    }

    public void setBploApprovedBy(String bploApprovedBy) {
        this.bploApprovedBy = bploApprovedBy;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Boolean getMarketSupervisorApproved() {
        return marketSupervisorApproved;
    }

    public void setMarketSupervisorApproved(Boolean marketSupervisorApproved) {
        this.marketSupervisorApproved = Boolean.TRUE.equals(marketSupervisorApproved);
        this.marketApprovalStatus = this.marketSupervisorApproved ? "APPROVED" : "PENDING";
    }

    public Boolean getBploApproved() {
        return bploApproved;
    }

    public void setBploApproved(Boolean bploApproved) {
        this.bploApproved = Boolean.TRUE.equals(bploApproved);
        this.bploStatus = this.bploApproved ? "APPROVED" : "PENDING";
    }

    public Boolean getEndorsingApproved() {
        return endorsingApproved;
    }

    public void setEndorsingApproved(Boolean endorsingApproved) {
        this.endorsingApproved = Boolean.TRUE.equals(endorsingApproved);
        this.endorsementStatus = this.endorsingApproved ? "APPROVED" : "PENDING";
    }

    public Boolean getApplicantFeePaid() {
        return applicantFeePaid;
    }

    public void setApplicantFeePaid(Boolean applicantFeePaid) {
        this.applicantFeePaid = Boolean.TRUE.equals(applicantFeePaid);
    }

    public BigDecimal getApplicantFeeAmount() {
        return applicantFeeAmount;
    }

    public void setApplicantFeeAmount(BigDecimal applicantFeeAmount) {
        this.applicantFeeAmount =
                applicantFeeAmount == null ? BigDecimal.ZERO : applicantFeeAmount;
    }

    public LocalDate getApplicantFeeDate() {
        return applicantFeeDate;
    }

    public void setApplicantFeeDate(LocalDate applicantFeeDate) {
        this.applicantFeeDate = applicantFeeDate;
    }

    public Boolean getVerifiedApplication() {
        return verifiedApplication;
    }

    public void setVerifiedApplication(Boolean verifiedApplication) {
        this.verifiedApplication = Boolean.TRUE.equals(verifiedApplication);
    }

    public Boolean getVerifiedStakeholder() {
        return verifiedApplication;
    }

    public Boolean getVerifiedTenant() {
        return verifiedApplication;
    }

    private String normalizeOnboardingStatus(String status) {
        if (status == null || status.isBlank()) {
            return "NEW";
        }

        String normalized = status.trim().toUpperCase();

        if (
                "NEW".equals(normalized)
                        || "BUSINESS_SUBMITTED".equals(normalized)
                        || "PAYMENT_PENDING".equals(normalized)
                        || "FOR_APPROVAL".equals(normalized)
                        || "APPROVED".equals(normalized)
                        || "REJECTED".equals(normalized)
        ) {
            return normalized;
        }

        if (
                "APPLICANT".equals(normalized)
                        || "ADVANCE_PAYMENT_COMPLETED".equals(normalized)
                        || "MARKET_APPROVED".equals(normalized)
                        || "STALL_ASSIGNED".equals(normalized)
                        || "CONTRACT_CREATED".equals(normalized)
                        || "BPLO_APPROVED".equals(normalized)
                        || "ENDORSED".equals(normalized)
                        || "TREASURER_PAID".equals(normalized)
        ) {
            return "FOR_APPROVAL";
        }

        if (
                "VERIFIED_STAKEHOLDER".equals(normalized)
                        || "ACTIVE_OCCUPANT".equals(normalized)
        ) {
            return "APPROVED";
        }

        return "NEW";
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getIdFileName() {
        return idFileName;
    }

    public void setIdFileName(String idFileName) {
        this.idFileName = idFileName;
    }

    public String getIdFilePath() {
        return idFilePath;
    }

    public void setIdFilePath(String idFilePath) {
        this.idFilePath = idFilePath;
    }

    public String getLetterFileName() {
        return letterFileName;
    }

    public void setLetterFileName(String letterFileName) {
        this.letterFileName = letterFileName;
    }

    public String getLetterFilePath() {
        return letterFilePath;
    }

    public void setLetterFilePath(String letterFilePath) {
        this.letterFilePath = letterFilePath;
    }

    public LocalDate getAppliedOn() {
        return appliedOn;
    }

    public void setAppliedOn(LocalDate appliedOn) {
        this.appliedOn = appliedOn;
    }

    public LocalDate getApprovedOn() {
        return approvedOn;
    }

    public void setApprovedOn(LocalDate approvedOn) {
        this.approvedOn = approvedOn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
