package com.code.back_end.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "billings",
        indexes = {

                @Index(
                        name = "idx_billing_occupant",
                        columnList = "occupant_id"
                ),

                @Index(
                        name = "idx_billing_contract",
                        columnList = "contract_id"
                ),

                @Index(
                        name = "idx_billing_due_date",
                        columnList = "due_date"
                ),

                @Index(
                        name = "idx_billing_status",
                        columnList = "status"
                ),

                @Index(
                        name = "idx_billing_balance",
                        columnList = "balance"
                )
        }
)
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // OCCUPANT
    // =========================
    @ManyToOne
    @JoinColumn(name = "occupant_id")
    private Occupant occupant;

    // =========================
    // CONTRACT
    // =========================
    @ManyToOne
    @JoinColumn(name = "contract_id")
    private Contract contract;

    private String billingNo;

    private String billingPeriod;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount =
            BigDecimal.ZERO;

    private BigDecimal balance;

    private LocalDate dueDate;

    private String status = "UNPAID";

    private LocalDateTime createdAt =
            LocalDateTime.now();

    @JsonIgnore
    @OneToMany(mappedBy = "billing")
    private List<Payment> payments;

    // =========================
    // GETTERS & SETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Contract getContract() {
        return contract;
    }

    public void setContract(
            Contract contract
    ) {
        this.contract = contract;
    }

    public String getBillingNo() {
        return billingNo;
    }

    public void setBillingNo(
            String billingNo
    ) {
        this.billingNo = billingNo;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(
            String billingPeriod
    ) {
        this.billingPeriod = billingPeriod;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(
            BigDecimal totalAmount
    ) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(
            BigDecimal paidAmount
    ) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(
            BigDecimal balance
    ) {
        this.balance = balance;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(
            LocalDate dueDate
    ) {
        this.dueDate = dueDate;
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

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(
            List<Payment> payments
    ) {
        this.payments = payments;
    }
}