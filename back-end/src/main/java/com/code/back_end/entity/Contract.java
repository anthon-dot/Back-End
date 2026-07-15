package com.code.back_end.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "contracts",
        indexes = {

                @Index(
                        name = "idx_contract_occupant",
                        columnList = "occupant_id"
                ),

                @Index(
                        name = "idx_contract_stall",
                        columnList = "stall_id"
                ),

                @Index(
                        name = "idx_contract_status",
                        columnList = "status"
                ),

                @Index(
                        name = "idx_contract_start_date",
                        columnList = "start_date"
                ),

                @Index(
                        name = "idx_contract_end_date",
                        columnList = "end_date"
                ),

                @Index(
                        name = "idx_contract_status_end_date",
                        columnList = "status,end_date"
                )
        }
)
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // OCCUPANT
    // =========================
    @ManyToOne
    @JoinColumn(name = "occupant_id")
    @JsonIgnoreProperties({"stall", "stakeholder"})
    @NotNull(message = "Occupant is required")
    private Occupant occupant;

    // =========================
    // STALL
    // =========================
    @ManyToOne
    @JoinColumn(name = "stall_id")
    @JsonIgnoreProperties("occupant")
    @NotNull(message = "Stall is required")
    private Stall stall;

    @Size(max = 80)
    private String contractNo;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Positive(message = "Monthly rent must be greater than zero")
    private BigDecimal monthlyRent;

    @NotBlank(message = "Billing frequency is required")
    @Size(max = 30)
    private String billingFrequency =
            "MONTHLY";

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Size(max = 30)
    private String status = "ACTIVE";

    private LocalDateTime createdAt =
            LocalDateTime.now();

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

    public Occupant getOccupant() {
        return occupant;
    }

    public void setOccupant(
            Occupant occupant
    ) {
        this.occupant = occupant;
    }

    public Stall getStall() {
        return stall;
    }

    public void setStall(
            Stall stall
    ) {
        this.stall = stall;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(
            String contractNo
    ) {
        this.contractNo = contractNo;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(
            LocalDate startDate
    ) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(
            LocalDate endDate
    ) {
        this.endDate = endDate;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(
            BigDecimal monthlyRent
    ) {
        this.monthlyRent = monthlyRent;
    }

    public String getBillingFrequency() {
        return billingFrequency;
    }

    public void setBillingFrequency(
            String billingFrequency
    ) {
        this.billingFrequency =
                billingFrequency;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(
            String terms
    ) {
        this.terms = terms;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status
    ) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}
