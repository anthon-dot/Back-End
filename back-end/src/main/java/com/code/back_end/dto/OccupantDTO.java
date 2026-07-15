package com.code.back_end.dto;

import com.code.back_end.dto.StallDTO;
import com.code.back_end.entity.Occupant;
import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.Stall;

import java.time.LocalDateTime;
import java.time.LocalDate;

public class OccupantDTO {

    private Long id;

    private Long stakeholderId;

    private String businessName;
    private String businessType;

    private String firstName;
    private String middleName;
    private String lastName;

    private String contact;
    private String address;

    private String applicationStatus;
    private String marketApprovalStatus;
    private String endorsementStatus;
    private String bploStatus;

    private Boolean isArchived;

    private LocalDateTime occupiedSince;
    private LocalDate occupancyDate;
    private String status;
    private Long contractId;

    // 🔥 STALL ADDED
    private StallDTO stall;

    public OccupantDTO(Occupant occupant) {

        if (occupant == null) return;

        this.id = occupant.getId();
        this.isArchived = occupant.getIsArchived();
        this.occupiedSince = occupant.getOccupiedSince();
        this.occupancyDate = occupant.getOccupancyDate();
        this.status = occupant.getStatus();
        this.contractId = occupant.getContractId();

        // =========================
        // STAKEHOLDER DATA
        // =========================
        Stakeholder stakeholder = occupant.getStakeholder();

        if (stakeholder != null) {

            this.stakeholderId = stakeholder.getId();
            this.businessName = stakeholder.getBusinessName();
            this.businessType = stakeholder.getBusinessType();

            this.firstName = stakeholder.getFirstName();
            this.middleName = stakeholder.getMiddleName();
            this.lastName = stakeholder.getLastName();

            this.contact = stakeholder.getContact();
            this.address = stakeholder.getAddress();

            this.applicationStatus = stakeholder.getApplicationStatus();
            this.marketApprovalStatus = stakeholder.getMarketApprovalStatus();
            this.endorsementStatus = stakeholder.getEndorsementStatus();
            this.bploStatus = stakeholder.getBploStatus();
        }

        // =========================
        // STALL DATA (IMPORTANT FIX)
        // =========================
        Stall stallEntity = occupant.getStall();

        if (stallEntity != null) {
            this.stall = new StallDTO(stallEntity);
        }
    }

    // =========================
    // GETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public Long getStakeholderId() {
        return stakeholderId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getContact() {
        return contact;
    }

    public String getAddress() {
        return address;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public String getMarketApprovalStatus() {
        return marketApprovalStatus;
    }

    public String getEndorsementStatus() {
        return endorsementStatus;
    }

    public String getBploStatus() {
        return bploStatus;
    }

    public Boolean getIsArchived() {
        return isArchived;
    }

    public LocalDateTime getOccupiedSince() {
        return occupiedSince;
    }

    public LocalDate getOccupancyDate() {
        return occupancyDate;
    }

    public String getStatus() {
        return status;
    }

    public Long getContractId() {
        return contractId;
    }

    // 🔥 IMPORTANT GETTER FOR JSON SERIALIZATION
    public StallDTO getStall() {
        return stall;
    }
}
