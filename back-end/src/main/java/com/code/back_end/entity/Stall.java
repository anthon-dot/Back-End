package com.code.back_end.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stalls")
public class Stall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String stallNo;

    @Column(nullable = false)
    private String stallType;

    @Column(nullable = false)
    private Double monthlyRent;

    @Column(nullable = false)
    private String status = "AVAILABLE";

    private String imageUrl;

    @Column(length = 500)
    private String info;

    private Double latitude;

    private Double longitude;
    

// ================================
    // OCCUPANT RELATIONSHIP (UPDATED)
    // ================================
    @OneToOne
    @JoinColumn(name = "occupant_id")
    @JsonBackReference(value = "occupant-stall") // Named reference avoids confusion
    private Occupant occupant;

    private LocalDateTime createdAt =
            LocalDateTime.now();

    // GETTERS & SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStallNo() {
        return stallNo;
    }

    public void setStallNo(String stallNo) {
        this.stallNo = stallNo;
    }

    public String getStallType() {
        return stallType;
    }

    public void setStallType(String stallType) {
        this.stallType = stallType;
    }

    public Double getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(Double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status == null || status.isBlank()) {
            this.status = "AVAILABLE";
            return;
        }

        String normalized = status.trim().toUpperCase();
        this.status = switch (normalized) {
            case "VACANT" -> "AVAILABLE";
            case "AVAILABLE", "OCCUPIED", "MAINTENANCE" -> normalized;
            default -> "AVAILABLE";
        };
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Occupant getOccupant() {
        return occupant;
    }

    public void setOccupant(Occupant occupant) {
        this.occupant = occupant;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
