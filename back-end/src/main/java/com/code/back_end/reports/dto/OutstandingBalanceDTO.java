package com.code.back_end.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OutstandingBalanceDTO {

    private String billingNo;
    private String businessName;
    private BigDecimal balance;
    private LocalDate dueDate;

    public OutstandingBalanceDTO(
            String billingNo,
            String businessName,
            BigDecimal balance,
            LocalDate dueDate
    ) {
        this.billingNo = billingNo;
        this.businessName = businessName;
        this.balance = balance;
        this.dueDate = dueDate;
    }

    public String getBillingNo() {
        return billingNo;
    }

    public String getBusinessName() {
        return businessName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }
}