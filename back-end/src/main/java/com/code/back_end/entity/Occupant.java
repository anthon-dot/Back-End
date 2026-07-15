package com.code.back_end.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "occupants",
        indexes = {

                @Index(
                        name = "idx_occupant_stakeholder",
                        columnList = "stakeholder_id"
                ),

                @Index(
                        name = "idx_occupant_archived",
                        columnList = "is_archived"
                )
        }
)
public class Occupant {

    @Id
    @GeneratedValue(strategy =
            GenerationType.IDENTITY)
    private Long id;

    // =========================
    // APPROVED STAKEHOLDER
    // =========================
    @OneToOne
    @JoinColumn(name = "stakeholder_id")
    @JsonBackReference(
            value = "stakeholder-occupant"
    )
    private Stakeholder stakeholder;

    // =========================
    // ASSIGNED STALL
    // =========================
    @OneToOne(mappedBy = "occupant")
    @JsonManagedReference(
            value = "occupant-stall"
    )
    private Stall stall;

    @Column(nullable = false)
    private BigDecimal advanceBalance =
            BigDecimal.ZERO;

    private LocalDateTime occupiedSince =
            LocalDateTime.now();

    private LocalDate occupancyDate;

    private String status = "PENDING";

    private Long contractId;

    private Boolean isArchived = false;

    // =========================
    // GETTERS & SETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Stakeholder getStakeholder() {
        return stakeholder;
    }

    public void setStakeholder(
            Stakeholder stakeholder
    ) {
        this.stakeholder = stakeholder;
    }

    public Stall getStall() {
        return stall;
    }

    public void setStall(
            Stall stall
    ) {
        this.stall = stall;
    }

    public BigDecimal getAdvanceBalance() {
        return advanceBalance;
    }

    public void setAdvanceBalance(
            BigDecimal advanceBalance
    ) {
        this.advanceBalance =
                advanceBalance;
    }

    public LocalDateTime getOccupiedSince() {
        return occupiedSince;
    }

    public void setOccupiedSince(
            LocalDateTime occupiedSince
    ) {
        this.occupiedSince =
                occupiedSince;
    }

    public LocalDate getOccupancyDate() {
        return occupancyDate;
    }

    public void setOccupancyDate(
            LocalDate occupancyDate
    ) {
        this.occupancyDate =
                occupancyDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status
    ) {
        this.status =
                status;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(
            Long contractId
    ) {
        this.contractId =
                contractId;
    }

    public Boolean getIsArchived() {
        return isArchived;
    }

    public void setIsArchived(
            Boolean archived
    ) {
        isArchived = archived;
    }
}
