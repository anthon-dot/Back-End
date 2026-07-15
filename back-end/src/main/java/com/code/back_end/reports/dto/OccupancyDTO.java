package com.code.back_end.reports.dto;

public class OccupancyDTO {

    private String status;
    private Long total;

    public OccupancyDTO(
            String status,
            Long total
    ) {
        this.status = status;
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public Long getTotal() {
        return total;
    }
}