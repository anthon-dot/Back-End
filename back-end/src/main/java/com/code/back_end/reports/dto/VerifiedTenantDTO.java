package com.code.back_end.reports.dto;

import java.time.LocalDate;

public class VerifiedTenantDTO {
    private String businessName;
    private String ownerName;
    private LocalDate approvedOn;

    public VerifiedTenantDTO(String businessName, String ownerName, LocalDate approvedOn) {
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.approvedOn = approvedOn;
    }

    public String getBusinessName() { return businessName; }
    public String getOwnerName() { return ownerName; }
    public LocalDate getApprovedOn() { return approvedOn; }
}
