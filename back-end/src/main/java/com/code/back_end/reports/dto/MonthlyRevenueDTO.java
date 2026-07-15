package com.code.back_end.reports.dto;

import java.math.BigDecimal;

public class MonthlyRevenueDTO {

    private Integer year;
    private Integer month;
    private BigDecimal totalRevenue;

    public MonthlyRevenueDTO(
            Integer year,
            Integer month,
            BigDecimal totalRevenue
    ) {
        this.year = year;
        this.month = month;
        this.totalRevenue = totalRevenue;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }
}