package com.code.back_end.reports.dto;

import java.time.LocalDate;

public class NewApplicationDTO {
    private String businessName;
    private LocalDate appliedOn;
    private String applicationStatus;

    public NewApplicationDTO(String businessName, LocalDate appliedOn, String applicationStatus) {
        this.businessName = businessName;
        this.appliedOn = appliedOn;
        this.applicationStatus = applicationStatus;
    }

    public String getBusinessName() { return businessName; }
    public LocalDate getAppliedOn() { return appliedOn; }
    public String getApplicationStatus() { return applicationStatus; }
}
