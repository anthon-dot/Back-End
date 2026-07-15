package com.code.back_end.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AIReportDTO {

    private String reportType;
    private String title;
    private String narrative;
    private String riskLevel;
    private BigDecimal expectedRevenue = BigDecimal.ZERO;
    private BigDecimal collectedRevenue = BigDecimal.ZERO;
    private BigDecimal outstandingBalance = BigDecimal.ZERO;
    private double occupancyRate;
    private double collectionEfficiency;
    private List<String> highlights = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private Map<String, Object> data;

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getExpectedRevenue() {
        return expectedRevenue;
    }

    public void setExpectedRevenue(BigDecimal expectedRevenue) {
        this.expectedRevenue = expectedRevenue;
    }

    public BigDecimal getCollectedRevenue() {
        return collectedRevenue;
    }

    public void setCollectedRevenue(BigDecimal collectedRevenue) {
        this.collectedRevenue = collectedRevenue;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public double getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public double getCollectionEfficiency() {
        return collectionEfficiency;
    }

    public void setCollectionEfficiency(double collectionEfficiency) {
        this.collectionEfficiency = collectionEfficiency;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<String> highlights) {
        this.highlights = highlights;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
