package com.code.back_end.reports.dto;

public class ExpensiveStallDTO {
    private String stallNo;
    private String stallType;
    private Double monthlyRent;

    public ExpensiveStallDTO(String stallNo, String stallType, Double monthlyRent) {
        this.stallNo = stallNo;
        this.stallType = stallType;
        this.monthlyRent = monthlyRent;
    }

    public String getStallNo() { return stallNo; }
    public String getStallType() { return stallType; }
    public Double getMonthlyRent() { return monthlyRent; }
}
