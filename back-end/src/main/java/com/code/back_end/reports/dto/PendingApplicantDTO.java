package com.code.back_end.reports.dto;

import java.time.LocalDate;

public class PendingApplicantDTO {
    private String businessName;
    private String ownerName;
    private LocalDate appliedOn;

    public PendingApplicantDTO(String businessName, String ownerName, LocalDate appliedOn) {
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.appliedOn = appliedOn;
    }

    public String getBusinessName() { return businessName; }
    public String getOwnerName() { return ownerName; }
    public LocalDate getAppliedOn() { return appliedOn; }
}
