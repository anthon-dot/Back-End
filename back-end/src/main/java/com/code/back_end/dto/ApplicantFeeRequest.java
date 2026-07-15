package com.code.back_end.dto;

import java.math.BigDecimal;

public class ApplicantFeeRequest {

    private BigDecimal amount;
    private String referenceNo;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }
}
