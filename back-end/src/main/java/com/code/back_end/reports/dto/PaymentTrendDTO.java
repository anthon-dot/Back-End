package com.code.back_end.reports.dto;

import java.math.BigDecimal;

public class PaymentTrendDTO {
    private Integer year;
    private Integer month;
    private BigDecimal totalAmount;

    public PaymentTrendDTO(Integer year, Integer month, BigDecimal totalAmount) {
        this.year = year;
        this.month = month;
        this.totalAmount = totalAmount;
    }

    public Integer getYear() { return year; }
    public Integer getMonth() { return month; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
