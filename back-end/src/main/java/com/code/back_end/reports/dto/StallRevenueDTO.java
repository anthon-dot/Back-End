package com.code.back_end.reports.dto;

import java.math.BigDecimal;

public class StallRevenueDTO {
    private String stallNo;
    private BigDecimal totalRevenue;

    public StallRevenueDTO(String stallNo, BigDecimal totalRevenue) {
        this.stallNo = stallNo;
        this.totalRevenue = totalRevenue;
    }

    public String getStallNo() { return stallNo; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
}
