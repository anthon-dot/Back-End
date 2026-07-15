package com.code.back_end.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaymentAnalysisDTO {

    private BigDecimal currentMonthCollected = BigDecimal.ZERO;
    private BigDecimal previousMonthCollected = BigDecimal.ZERO;
    private double percentageChange;
    private String summary;
    private List<AITrendDTO> monthlyTrends = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();

    public BigDecimal getCurrentMonthCollected() {
        return currentMonthCollected;
    }

    public void setCurrentMonthCollected(BigDecimal currentMonthCollected) {
        this.currentMonthCollected = currentMonthCollected;
    }

    public BigDecimal getPreviousMonthCollected() {
        return previousMonthCollected;
    }

    public void setPreviousMonthCollected(BigDecimal previousMonthCollected) {
        this.previousMonthCollected = previousMonthCollected;
    }

    public double getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(double percentageChange) {
        this.percentageChange = percentageChange;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<AITrendDTO> getMonthlyTrends() {
        return monthlyTrends;
    }

    public void setMonthlyTrends(List<AITrendDTO> monthlyTrends) {
        this.monthlyTrends = monthlyTrends;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}
