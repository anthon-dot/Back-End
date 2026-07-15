package com.code.back_end.reports.dto;

import java.time.LocalDate;

public class ArchivedTenantDTO {
    private String businessName;
    private String ownerName;
    private LocalDate archivedOn;

    public ArchivedTenantDTO(String businessName, String ownerName, LocalDate archivedOn) {
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.archivedOn = archivedOn;
    }

    public String getBusinessName() { return businessName; }
    public String getOwnerName() { return ownerName; }
    public LocalDate getArchivedOn() { return archivedOn; }
}
