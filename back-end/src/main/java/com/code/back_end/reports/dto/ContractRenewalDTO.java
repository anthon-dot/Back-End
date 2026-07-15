package com.code.back_end.reports.dto;

import java.time.LocalDate;

public class ContractRenewalDTO {
    private String contractNo;
    private String businessName;
    private LocalDate startDate;

    public ContractRenewalDTO(String contractNo, String businessName, LocalDate startDate) {
        this.contractNo = contractNo;
        this.businessName = businessName;
        this.startDate = startDate;
    }

    public String getContractNo() { return contractNo; }
    public String getBusinessName() { return businessName; }
    public LocalDate getStartDate() { return startDate; }
}
