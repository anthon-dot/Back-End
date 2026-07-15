package com.code.back_end.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillingDTO {

    private Long id;

    private String billingNo;

    private String billingPeriod;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private BigDecimal balance;

    private LocalDate dueDate;
    private String billingFrequency;

    public String getBillingFrequency() {
        return billingFrequency;
    }

    public void setBillingFrequency(String billingFrequency) {
        this.billingFrequency = billingFrequency;
    }

    private String status;

    private String occupantName;

    // =========================
    // ADD THESE
    // =========================
    private Long stakeholderId;

    private Long contractId;

    // =========================
    // GETTERS & SETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillingNo() {
        return billingNo;
    }

    public void setBillingNo(
            String billingNo
    ) {
        this.billingNo = billingNo;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(
            String billingPeriod
    ) {
        this.billingPeriod =
                billingPeriod;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(
            BigDecimal totalAmount
    ) {
        this.totalAmount =
                totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(
            BigDecimal paidAmount
    ) {
        this.paidAmount =
                paidAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(
            BigDecimal balance
    ) {
        this.balance = balance;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(
            LocalDate dueDate
    ) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status
    ) {
        this.status = status;
    }

    public String getOccupantName() {
        return occupantName;
    }

    public void setOccupantName(
            String occupantName
    ) {
        this.occupantName =
                occupantName;
    }

    public Long getStakeholderId() {
        return stakeholderId;
    }

    public void setStakeholderId(
            Long stakeholderId
    ) {
        this.stakeholderId =
                stakeholderId;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(
            Long contractId
    ) {
        this.contractId =
                contractId;
    }
}