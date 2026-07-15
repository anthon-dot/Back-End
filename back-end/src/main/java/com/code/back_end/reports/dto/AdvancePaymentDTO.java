package com.code.back_end.reports.dto;

import java.math.BigDecimal;

public class AdvancePaymentDTO {
    private String businessName;
    private String ownerName;
    private BigDecimal advanceAmount;
    private BigDecimal advanceBalance;

    public AdvancePaymentDTO(String businessName, String ownerName, BigDecimal advanceAmount, BigDecimal advanceBalance) {
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.advanceAmount = advanceAmount;
        this.advanceBalance = advanceBalance;
    }

    public String getBusinessName() { return businessName; }
    public String getOwnerName() { return ownerName; }
    public BigDecimal getAdvanceAmount() { return advanceAmount; }
    public BigDecimal getAdvanceBalance() { return advanceBalance; }
}
