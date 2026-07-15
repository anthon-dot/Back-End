package com.code.back_end.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AIReportSummaryDTO {

    private BigDecimal totalCollected = BigDecimal.ZERO;
    private BigDecimal outstandingBalance = BigDecimal.ZERO;
    private long overdueTenants;
    private long pendingApplications;
    private double occupancyRate;
    private String summary;
    private List<String> recommendations = new ArrayList<>();
    private List<AIInsightDTO> insights = new ArrayList<>();
    private List<AITrendDTO> monthlyTrends = new ArrayList<>();

    public BigDecimal getTotalCollected() {
        return totalCollected;
    }

    public void setTotalCollected(BigDecimal totalCollected) {
        this.totalCollected = totalCollected;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public long getOverdueTenants() {
        return overdueTenants;
    }

    public void setOverdueTenants(long overdueTenants) {
        this.overdueTenants = overdueTenants;
    }

    public long getPendingApplications() {
        return pendingApplications;
    }

    public void setPendingApplications(long pendingApplications) {
        this.pendingApplications = pendingApplications;
    }

    public double getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public List<AIInsightDTO> getInsights() {
        return insights;
    }

    public void setInsights(List<AIInsightDTO> insights) {
        this.insights = insights;
    }

    public List<AITrendDTO> getMonthlyTrends() {
        return monthlyTrends;
    }

    public void setMonthlyTrends(List<AITrendDTO> monthlyTrends) {
        this.monthlyTrends = monthlyTrends;
    }
}
