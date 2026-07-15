package com.code.back_end.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "stakeholders",
        indexes = {

                @Index(
                        name = "idx_stakeholder_business_name",
                        columnList = "business_name"
                ),

                @Index(
                        name = "idx_stakeholder_business_type",
                        columnList = "business_type"
                ),

                @Index(
                        name = "idx_stakeholder_application_status",
                        columnList = "application_status"
                ),

                @Index(
                        name = "idx_stakeholder_onboarding_status",
                        columnList = "onboarding_status"
                ),

                @Index(
                        name = "idx_stakeholder_verified_tenant",
                        columnList = "verified_tenant"
                ),

                @Index(
                        name = "idx_stakeholder_archived",
                        columnList = "is_archived"
                ),

                @Index(
                        name = "idx_stakeholder_approved_on",
                        columnList = "approved_on"
                ),

                @Index(
                        name = "idx_stakeholder_applied_on",
                        columnList = "applied_on"
                ),

                @Index(
                        name = "idx_stakeholder_created_at",
                        columnList = "created_at"
                ),

                @Index(
                        name = "idx_stakeholder_advance_payment_amount",
                        columnList = "advance_payment_amount"
                ),

                @Index(
                        name = "idx_stakeholder_advance_balance",
                        columnList = "advance_balance"
                ),

                @Index(
                        name = "idx_stakeholder_application_archived",
                        columnList = "application_status,is_archived"
                )
        }
)
public class Stakeholder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================
    // OCCUPANT
    // =====================================
    @OneToOne(
            mappedBy = "stakeholder",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER
    )
    @JsonManagedReference(
            value = "stakeholder-occupant"
    )
    private Occupant occupant;

    // =====================================
    // USER
    // =====================================
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    // =====================================
    // BUSINESS INFO
    // =====================================
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

    // =====================================
    // APPLICATION STATUS
    // =====================================
    private String applicationStatus =
            "PENDING";

    @Column(nullable = false, columnDefinition = "varchar(50) default 'NEW'")
    private String onboardingStatus = "NEW";

    @Column(columnDefinition = "TEXT")
    private String notes;

    // =====================================
    // APPLICANT REQUIREMENTS
    // =====================================

    @Column(nullable = false)
    private Boolean applicationFormPaid =
            false;

    @Column(nullable = false)
    private Boolean advancePaymentCompleted =
            false;

    @Column(nullable = false)
    private Boolean advancePaymentPaid =
            false;

    @Column(nullable = false)
    private BigDecimal advancePaymentAmount =
            BigDecimal.ZERO;

    private LocalDate advancePaymentDate;

    @Column(nullable = false)
    private BigDecimal advanceBalance =
            BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalAdvanceAmount =
            BigDecimal.ZERO;

    // =====================================
    // VERIFIED TENANT FLAG
    // =====================================
    @Column(nullable = false)
    private Boolean verifiedTenant =
            false;

    @Column(nullable = false)
    private Boolean verifiedStakeholder =
            false;

    @Column(nullable = false)
    private Boolean applicantFeePaid =
            false;

    @Column(nullable = false)
    private Boolean treasurerApproved =
            false;

    private BigDecimal applicantFeeAmount =
            BigDecimal.ZERO;

    private LocalDate applicantFeeDate;

    private LocalDateTime verificationDate;

    // =====================================
    // APPROVALS
    // =====================================
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
    private Boolean marketSupervisorApproved =
            false;

    @Column(nullable = false)
    private Boolean bploApproved =
            false;

    @Column(nullable = false)
    private Boolean endorsingApproved =
            false;

    @Column(nullable = false)
    private Boolean finalEndorsed =
            false;

    @Column(nullable = false)
    private Boolean treasurerPaid =
            false;

    // =====================================
    // DATES
    // =====================================
    private LocalDate appliedOn;

    private LocalDate approvedOn;

    private LocalDate archivedOn;

    private Boolean isArchived =
            false;

    private LocalDateTime createdAt =
            LocalDateTime.now();

    // =====================================
    // DOCUMENTS
    // =====================================
    @OneToMany(
            mappedBy = "stakeholder",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonManagedReference
    private List<StakeholderDocument> documents;

    // =====================================
    // GETTERS & SETTERS
    // =====================================

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Occupant getOccupant() {
        return occupant;
    }

    public void setOccupant(
            Occupant occupant
    ) {
        this.occupant = occupant;
    }

    public User getUser() {
        return user;
    }

    public void setUser(
            User user
    ) {
        this.user = user;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(
            String businessName
    ) {
        this.businessName =
                businessName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(
            String businessType
    ) {
        this.businessType =
                businessType;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(
            String firstName
    ) {
        this.firstName =
                firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(
            String middleName
    ) {
        this.middleName =
                middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(
            String lastName
    ) {
        this.lastName =
                lastName;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(
            String contact
    ) {
        this.contact =
                contact;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(
            String email
    ) {
        this.email =
                email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(
            String address
    ) {
        this.address =
                address;
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

    public void setApplicationStatus(
            String applicationStatus
    ) {
        this.applicationStatus =
                applicationStatus;
    }

    public String getOnboardingStatus() {
        return onboardingStatus;
    }

    public void setOnboardingStatus(
            String onboardingStatus
    ) {
        this.onboardingStatus =
                normalizeOnboardingStatus(onboardingStatus);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(
            String notes
    ) {
        this.notes =
                notes;
    }

    public Boolean getApplicationFormPaid() {
        return applicationFormPaid;
    }

    public void setApplicationFormPaid(
            Boolean applicationFormPaid
    ) {
        this.applicationFormPaid =
                applicationFormPaid;
    }

    public Boolean getAdvancePaymentCompleted() {
        return advancePaymentCompleted;
    }

    public void setAdvancePaymentCompleted(
            Boolean advancePaymentCompleted
    ) {
        this.advancePaymentCompleted =
                advancePaymentCompleted;
        this.advancePaymentPaid =
                Boolean.TRUE.equals(advancePaymentCompleted);
    }

    public Boolean getAdvancePayment() {
        return advancePaymentCompleted;
    }

    public void setAdvancePayment(
            Boolean advancePayment
    ) {
        this.advancePaymentCompleted =
                advancePayment;
        this.advancePaymentPaid =
                Boolean.TRUE.equals(advancePayment);
    }

    public Boolean getAdvancePaymentPaid() {
        return advancePaymentPaid;
    }

    public void setAdvancePaymentPaid(
            Boolean advancePaymentPaid
    ) {
        this.advancePaymentPaid =
                advancePaymentPaid;
        this.advancePaymentCompleted =
                Boolean.TRUE.equals(advancePaymentPaid);
    }

    public BigDecimal getAdvancePaymentAmount() {
        return advancePaymentAmount;
    }

    public void setAdvancePaymentAmount(
            BigDecimal advancePaymentAmount
    ) {
        this.advancePaymentAmount =
                advancePaymentAmount;
    }

    public LocalDate getAdvancePaymentDate() {
        return advancePaymentDate;
    }

    public void setAdvancePaymentDate(
            LocalDate advancePaymentDate
    ) {
        this.advancePaymentDate =
                advancePaymentDate;
    }

    public BigDecimal getAdvanceBalance() {
        return advanceBalance;
    }

    public void setAdvanceBalance(
            BigDecimal advanceBalance
    ) {
        this.advanceBalance =
                advanceBalance;
    }

    public BigDecimal getTotalAdvanceAmount() {
        return totalAdvanceAmount;
    }

    public void setTotalAdvanceAmount(
            BigDecimal totalAdvanceAmount
    ) {
        this.totalAdvanceAmount =
                totalAdvanceAmount;
    }

    public Boolean getVerifiedTenant() {
        return verifiedTenant;
    }

    public void setVerifiedTenant(
            Boolean verifiedTenant
    ) {
        this.verifiedTenant =
                Boolean.TRUE.equals(verifiedTenant);
    }

    public Boolean getVerifiedStakeholder() {
        return verifiedStakeholder;
    }

    public void setVerifiedStakeholder(
            Boolean verifiedStakeholder
    ) {
        this.verifiedStakeholder =
                Boolean.TRUE.equals(verifiedStakeholder);
        this.verifiedTenant =
                Boolean.TRUE.equals(verifiedStakeholder);
    }

    public Boolean getVerified() {
        return verifiedStakeholder;
    }

    public void setVerified(Boolean verified) {
        setVerifiedStakeholder(verified);
    }

    public Boolean getApplicantFeePaid() {
        return applicantFeePaid;
    }

    public void setApplicantFeePaid(
            Boolean applicantFeePaid
    ) {
        this.applicantFeePaid =
                Boolean.TRUE.equals(applicantFeePaid);
    }

    public LocalDateTime getVerificationDate() {
        return verificationDate;
    }

    public void setVerificationDate(LocalDateTime verificationDate) {
        this.verificationDate = verificationDate;
    }

    public Boolean getTreasurerApproved() {
        return treasurerApproved;
    }

    public void setTreasurerApproved(
            Boolean treasurerApproved
    ) {
        this.treasurerApproved =
                Boolean.TRUE.equals(treasurerApproved);
    }

    public BigDecimal getApplicantFeeAmount() {
        return applicantFeeAmount;
    }

    public void setApplicantFeeAmount(
            BigDecimal applicantFeeAmount
    ) {
        this.applicantFeeAmount =
                applicantFeeAmount;
    }

    public LocalDate getApplicantFeeDate() {
        return applicantFeeDate;
    }

    public void setApplicantFeeDate(
            LocalDate applicantFeeDate
    ) {
        this.applicantFeeDate =
                applicantFeeDate;
    }

    public Boolean getMarketSupervisorApproved() {
        return marketSupervisorApproved;
    }

    public void setMarketSupervisorApproved(
            Boolean marketSupervisorApproved
    ) {
        this.marketSupervisorApproved =
                marketSupervisorApproved;
        this.marketApprovalStatus =
                Boolean.TRUE.equals(marketSupervisorApproved)
                        ? "APPROVED"
                        : "PENDING";
    }

    public String getMarketApprovalStatus() {
        return marketApprovalStatus;
    }

    public void setMarketApprovalStatus(
            String marketApprovalStatus
    ) {
        this.marketApprovalStatus =
                normalizeApprovalStatus(marketApprovalStatus);
        this.marketSupervisorApproved =
                isApprovedStatus(this.marketApprovalStatus);
    }

    public Boolean getBploApproved() {
        return bploApproved;
    }

    public void setBploApproved(
            Boolean bploApproved
    ) {
        this.bploApproved =
                bploApproved;
        this.bploStatus =
                Boolean.TRUE.equals(bploApproved)
                        ? "APPROVED"
                        : "PENDING";
    }

    public String getBploStatus() {
        return bploStatus;
    }

    public void setBploStatus(
            String bploStatus
    ) {
        this.bploStatus =
                normalizeApprovalStatus(bploStatus);
        this.bploApproved =
                isApprovedStatus(this.bploStatus);
    }

    public Boolean getEndorsingApproved() {
        return endorsingApproved;
    }

    public void setEndorsingApproved(
            Boolean endorsingApproved
    ) {
        this.endorsingApproved =
                Boolean.TRUE.equals(endorsingApproved);
        this.endorsementStatus =
                Boolean.TRUE.equals(endorsingApproved)
                        ? "APPROVED"
                        : "PENDING";
        this.endorsingStatus =
                Boolean.TRUE.equals(endorsingApproved)
                        ? "ENDORSED"
                        : "PENDING";
        this.finalEndorsed =
                Boolean.TRUE.equals(endorsingApproved);
    }

    public Boolean getFinalEndorsed() {
        return finalEndorsed;
    }

    public void setFinalEndorsed(
            Boolean finalEndorsed
    ) {
        this.finalEndorsed =
                Boolean.TRUE.equals(finalEndorsed);
        this.endorsingApproved =
                Boolean.TRUE.equals(finalEndorsed);
        this.endorsementStatus =
                Boolean.TRUE.equals(finalEndorsed)
                        ? "APPROVED"
                        : this.endorsementStatus;
    }

    public String getEndorsementStatus() {
        return endorsementStatus;
    }

    public void setEndorsementStatus(
            String endorsementStatus
    ) {
        this.endorsementStatus =
                normalizeApprovalStatus(endorsementStatus);
        this.endorsingApproved =
                isApprovedStatus(this.endorsementStatus);
        this.endorsingStatus =
                this.endorsingApproved
                        ? "ENDORSED"
                        : normalizeEndorsingStatus(this.endorsingStatus);
        this.finalEndorsed =
                Boolean.TRUE.equals(this.finalEndorsed)
                        || this.endorsingApproved;
        this.treasurerApproved =
                Boolean.TRUE.equals(this.treasurerApproved);
    }

    public String getEndorsingStatus() {
        return endorsingStatus;
    }

    public void setEndorsingStatus(String endorsingStatus) {
        this.endorsingStatus = normalizeEndorsingStatus(endorsingStatus);
        this.endorsingApproved =
                "ENDORSED".equals(this.endorsingStatus)
                        || isApprovedStatus(this.endorsementStatus);
        if ("REJECTED".equals(this.endorsingStatus)) {
            this.endorsementStatus = "REJECTED";
        }
    }

    public String getEndorsementRemarks() {
        return endorsementRemarks;
    }

    public void setEndorsementRemarks(
            String endorsementRemarks
    ) {
        this.endorsementRemarks =
                endorsementRemarks;
    }

    public LocalDateTime getEndorsedAt() {
        return endorsedAt;
    }

    public void setEndorsedAt(
            LocalDateTime endorsedAt
    ) {
        this.endorsedAt =
                endorsedAt;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = normalizeApprovalStatus(finalStatus);
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

    public Boolean getTreasurerPaid() {
        return treasurerPaid;
    }

    public void setTreasurerPaid(
            Boolean treasurerPaid
    ) {
        this.treasurerPaid =
                treasurerPaid;
    }

    public LocalDate getAppliedOn() {
        return appliedOn;
    }

    public void setAppliedOn(
            LocalDate appliedOn
    ) {
        this.appliedOn =
                appliedOn;
    }

    public LocalDate getApprovedOn() {
        return approvedOn;
    }

    public void setApprovedOn(
            LocalDate approvedOn
    ) {
        this.approvedOn =
                approvedOn;
    }

    public LocalDate getArchivedOn() {
        return archivedOn;
    }

    public void setArchivedOn(
            LocalDate archivedOn
    ) {
        this.archivedOn =
                archivedOn;
    }

    public Boolean getIsArchived() {
        return isArchived;
    }

    public void setIsArchived(
            Boolean archived
    ) {
        isArchived = archived;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt =
                createdAt;
    }

    public List<StakeholderDocument>
    getDocuments() {
        return documents;
    }

    public void setDocuments(
            List<StakeholderDocument> documents
    ) {
        this.documents =
                documents;
    }

    // =====================================
    // AUTO VERIFICATION STATUS HELPER
    // =====================================

    public void updateVerificationStatus() {

        boolean verified =
                Boolean.TRUE.equals(this.advancePaymentPaid)
                        && isApprovedStatus(this.marketApprovalStatus)
                        && isApprovedStatus(this.endorsementStatus)
                        && isApprovedStatus(this.bploStatus)
                        && Boolean.TRUE.equals(this.applicantFeePaid);

        this.verifiedTenant = verified;
        this.verifiedStakeholder = verified;
    }

    @PrePersist
    @PreUpdate
    @PostLoad
    private void syncApprovalFlags() {
        this.marketApprovalStatus =
                this.marketApprovalStatus == null
                        && Boolean.TRUE.equals(this.marketSupervisorApproved)
                        ? "APPROVED"
                        : normalizeApprovalStatus(this.marketApprovalStatus);
        this.endorsementStatus =
                this.endorsementStatus == null
                        && Boolean.TRUE.equals(this.endorsingApproved)
                        ? "APPROVED"
                        : normalizeApprovalStatus(this.endorsementStatus);
        this.endorsingStatus =
                Boolean.TRUE.equals(this.endorsingApproved)
                        ? "ENDORSED"
                        : normalizeEndorsingStatus(this.endorsingStatus);
        this.bploStatus =
                this.bploStatus == null
                        && Boolean.TRUE.equals(this.bploApproved)
                        ? "APPROVED"
                        : normalizeApprovalStatus(this.bploStatus);
        this.finalStatus =
                normalizeApprovalStatus(this.finalStatus);

        this.marketSupervisorApproved =
                isApprovedStatus(this.marketApprovalStatus);
        this.endorsingApproved =
                isApprovedStatus(this.endorsementStatus);
        this.bploApproved =
                isApprovedStatus(this.bploStatus);
        this.onboardingStatus =
                normalizeOnboardingStatus(this.onboardingStatus);
        this.applicantFeePaid =
                Boolean.TRUE.equals(this.applicantFeePaid);
        this.advancePaymentCompleted =
                Boolean.TRUE.equals(this.advancePaymentCompleted)
                        || Boolean.TRUE.equals(this.advancePaymentPaid);
        this.advancePaymentPaid =
                Boolean.TRUE.equals(this.advancePaymentPaid)
                        || Boolean.TRUE.equals(this.advancePaymentCompleted);
        this.verifiedStakeholder =
                Boolean.TRUE.equals(this.applicantFeePaid);
        this.verifiedTenant =
                Boolean.TRUE.equals(this.applicantFeePaid);

        if (Boolean.TRUE.equals(this.applicantFeePaid) && this.verificationDate == null) {
            this.verificationDate = LocalDateTime.now();
        }

        if (!Boolean.TRUE.equals(this.applicantFeePaid)) {
            this.verificationDate = null;
        }
    }

    private String normalizeApprovalStatus(
            String status
    ) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }

        String normalized =
                status.trim().toUpperCase();

        if (
                "APPROVED".equals(normalized)
                        || "REJECTED".equals(normalized)
                        || "PENDING".equals(normalized)
        ) {
            return normalized;
        }

        return "PENDING";
    }

    private String normalizeEndorsingStatus(
            String status
    ) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }

        String normalized =
                status.trim().toUpperCase();

        if (
                "ENDORSED".equals(normalized)
                        || "APPROVED".equals(normalized)
                        || "REJECTED".equals(normalized)
                        || "PENDING".equals(normalized)
        ) {
            return normalized;
        }

        return "PENDING";
    }

    private boolean isApprovedStatus(
            String status
    ) {
        return "APPROVED".equals(
                normalizeApprovalStatus(status)
        );
    }

    private String normalizeOnboardingStatus(
            String status
    ) {
        if (status == null || status.isBlank()) {
            return "NEW";
        }

        String normalized =
                status.trim().toUpperCase();

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
}
