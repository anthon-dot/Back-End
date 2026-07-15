package com.code.back_end.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyCollectionDTO {

    private LocalDate date;
    private Long totalTransactions;
    private BigDecimal totalAmount;

    // Standard constructor
    public DailyCollectionDTO(
            LocalDate date,
            Long totalTransactions,
            BigDecimal totalAmount
    ) {
        this.date = date;
        this.totalTransactions = totalTransactions;
        this.totalAmount = totalAmount;
    }

    // Constructor to bypass Hibernate untyped JPQL function issues
    public DailyCollectionDTO(
            Object dateObj,
            Long totalTransactions,
            Object totalAmountObj
    ) {
        if (dateObj instanceof java.sql.Date) {
            this.date = ((java.sql.Date) dateObj).toLocalDate();
        } else if (dateObj instanceof LocalDate) {
            this.date = (LocalDate) dateObj;
        } else if (dateObj instanceof java.time.LocalDateTime) {
            this.date = ((java.time.LocalDateTime) dateObj).toLocalDate();
        } else if (dateObj != null) {
            try {
                this.date = LocalDate.parse(dateObj.toString());
            } catch (Exception e) {
                this.date = LocalDate.now();
            }
        }
        
        this.totalTransactions = totalTransactions;
        
        if (totalAmountObj instanceof BigDecimal) {
            this.totalAmount = (BigDecimal) totalAmountObj;
        } else if (totalAmountObj instanceof Number) {
            this.totalAmount = BigDecimal.valueOf(((Number) totalAmountObj).doubleValue());
        } else {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    // Aliases to avoid breaking code that expects paymentDay/totalCollections
    public LocalDate getPaymentDay() {
        return date;
    }

    public BigDecimal getTotalCollections() {
        return totalAmount;
    }

    public Long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(Long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}