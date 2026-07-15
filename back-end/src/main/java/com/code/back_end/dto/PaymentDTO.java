package com.code.back_end.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDTO {

    private Long id;

    private Long stakeholderId;

    private String stakeholderName;

    private String businessName;

    private String stallNo;

    private BigDecimal amount;

    private String receiptNo;

    private String referenceNo;

    private String paymentType;

    private LocalDateTime paymentDate;

    // =========================
    // GETTERS & SETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStakeholderId() {
        return stakeholderId;
    }

    public void setStakeholderId(Long stakeholderId) {
        this.stakeholderId = stakeholderId;
    }

    public String getStakeholderName() {
        return stakeholderName;
    }

    public void setStakeholderName(
            String stakeholderName
    ) {
        this.stakeholderName =
                stakeholderName;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(
            String businessName
    ) {
        this.businessName =
                businessName;
    }

    public String getStallNo() {
        return stallNo;
    }

    public void setStallNo(
            String stallNo
    ) {
        this.stallNo = stallNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(
            BigDecimal amount
    ) {
        this.amount = amount;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(
            String receiptNo
    ) {
        this.receiptNo = receiptNo;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(
            String referenceNo
    ) {
        this.referenceNo = referenceNo;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(
            String paymentType
    ) {
        this.paymentType = paymentType;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(
            LocalDateTime paymentDate
    ) {
        this.paymentDate = paymentDate;
    }
}