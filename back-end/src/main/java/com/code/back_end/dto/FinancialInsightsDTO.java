package com.code.back_end.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FinancialInsightsDTO {

    private BigDecimal totalBilled = BigDecimal.ZERO;
    private BigDecimal totalCollected = BigDecimal.ZERO;
    private BigDecimal outstandingBalance = BigDecimal.ZERO;
    private double collectionRate;
    private String summary;
    private List<AIInsightDTO> insights = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();

    public BigDecimal getTotalBilled() {
        return totalBilled;
    }

    public void setTotalBilled(BigDecimal totalBilled) {
        this.totalBilled = totalBilled;
    }

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

    public double getCollectionRate() {
        return collectionRate;
    }

    public void setCollectionRate(double collectionRate) {
        this.collectionRate = collectionRate;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<AIInsightDTO> getInsights() {
        return insights;
    }

    public void setInsights(List<AIInsightDTO> insights) {
        this.insights = insights;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}
