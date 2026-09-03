package com.code.back_end.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_rates")
public class RentalRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stallType;

    @Column(nullable = false)
    private Double monthlyRate;

    @Column(length = 500)
    private String description;

    private String status = "ACTIVE";

    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStallType() { return stallType; }
    public void setStallType(String stallType) { this.stallType = stallType; }

    public Double getMonthlyRate() { return monthlyRate; }
    public void setMonthlyRate(Double monthlyRate) { this.monthlyRate = monthlyRate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
