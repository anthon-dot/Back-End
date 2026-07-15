package com.code.back_end.reports.dto;

public class StallTypeDistributionDTO {
    private String stallType;
    private Long count;

    public StallTypeDistributionDTO(String stallType, Long count) {
        this.stallType = stallType;
        this.count = count;
    }

    public String getStallType() { return stallType; }
    public Long getCount() { return count; }
}
