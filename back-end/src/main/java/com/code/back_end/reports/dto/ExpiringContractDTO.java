package com.code.back_end.reports.dto;

import java.time.LocalDate;

public class ExpiringContractDTO {
    private String contractNo;
    private String businessName;
    private LocalDate endDate;

    public ExpiringContractDTO(String contractNo, String businessName, LocalDate endDate) {
        this.contractNo = contractNo;
        this.businessName = businessName;
        this.endDate = endDate;
    }

    public String getContractNo() { return contractNo; }
    public String getBusinessName() { return businessName; }
    public LocalDate getEndDate() { return endDate; }
}
