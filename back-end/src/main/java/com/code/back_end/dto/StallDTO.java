package com.code.back_end.dto;

import com.code.back_end.entity.Stall;

public class StallDTO {

    private Long id;
    private String stallNo;
    private String stallType;
    private Double monthlyRent;
    private String status;
    private String imageUrl;
    private String info;
    private Double latitude;
    private Double longitude;

    public StallDTO(Stall stall) {

        this.id = stall.getId();
        this.stallNo = stall.getStallNo();
        this.stallType = stall.getStallType();
        this.monthlyRent = stall.getMonthlyRent();
        this.status = stall.getStatus();
        this.imageUrl = stall.getImageUrl();
        this.info = stall.getInfo();
        this.latitude = stall.getLatitude();
        this.longitude = stall.getLongitude();
    }

    public Long getId() {
        return id;
    }

    public String getStallNo() {
        return stallNo;
    }

    public String getStallType() {
        return stallType;
    }

    public Double getMonthlyRent() {
        return monthlyRent;
    }

    public String getStatus() {
        return status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getInfo() {
        return info;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}