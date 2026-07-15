package com.code.back_end.dto;

import java.util.ArrayList;
import java.util.List;

public class OccupancyAnalysisDTO {

    private long totalStalls;
    private long occupiedStalls;
    private long vacantStalls;
    private double occupancyRate;
    private String summary;
    private List<String> recommendations = new ArrayList<>();

    public long getTotalStalls() {
        return totalStalls;
    }

    public void setTotalStalls(long totalStalls) {
        this.totalStalls = totalStalls;
    }

    public long getOccupiedStalls() {
        return occupiedStalls;
    }

    public void setOccupiedStalls(long occupiedStalls) {
        this.occupiedStalls = occupiedStalls;
    }

    public long getVacantStalls() {
        return vacantStalls;
    }

    public void setVacantStalls(long vacantStalls) {
        this.vacantStalls = vacantStalls;
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
}
