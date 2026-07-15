package com.code.back_end.dto;

import java.math.BigDecimal;

public class TreasurerApprovalRequest {

    private BigDecimal amount;
    private BigDecimal totalAdvanceAmount;
    private String referenceNo;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTotalAdvanceAmount() {
        return totalAdvanceAmount;
    }

    public void setTotalAdvanceAmount(BigDecimal totalAdvanceAmount) {
        this.totalAdvanceAmount = totalAdvanceAmount;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }
}
