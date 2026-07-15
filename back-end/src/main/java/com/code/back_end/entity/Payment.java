package com.code.back_end.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {

                @Index(
                        name = "idx_payment_billing",
                        columnList = "billing_id"
                ),

                @Index(
                        name = "idx_payment_stakeholder",
                        columnList = "stakeholder_id"
                ),

                @Index(
                        name = "idx_payment_payment_type",
                        columnList = "payment_type"
                ),

                @Index(
                        name = "idx_payment_payment_date",
                        columnList = "payment_date"
                ),

                @Index(
                        name = "idx_payment_type_date",
                        columnList = "payment_type,payment_date"
                )
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rent_cycle")
    private String rentCycle;

    // =========================
    // BILLING
    // =========================
    @ManyToOne
    @JoinColumn(
            name = "billing_id",
            nullable = true
    )
    private Billing billing;

    // =========================
    // STAKEHOLDER
    // =========================
    @ManyToOne
    @JoinColumn(name = "stakeholder_id")
    @NotNull(message = "Stakeholder is required")
    private Stakeholder stakeholder;
    

    // =========================
    // PAYMENT DETAILS
    // =========================
    @Transient
    private BigDecimal totalAdvanceAmount;

    @Positive(message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    private String receiptNo;

    @Size(max = 120)
    private String referenceNo;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    private LocalDateTime paymentDate =
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

    public String getRentCycle() {
        return rentCycle;
    }

    public void setRentCycle(
            String rentCycle
    ) {
        this.rentCycle = rentCycle;
    }

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(
            Billing billing
    ) {
        this.billing = billing;
    }

    public Stakeholder getStakeholder() {
        return stakeholder;
    }

    public void setStakeholder(
            Stakeholder stakeholder
    ) {
        this.stakeholder = stakeholder;
    }

    public BigDecimal getTotalAdvanceAmount() {
        return totalAdvanceAmount;
    }

    public void setTotalAdvanceAmount(
            BigDecimal totalAdvanceAmount
    ) {
        this.totalAdvanceAmount =
                totalAdvanceAmount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(
            BigDecimal amount
    ) {
        this.amount = amount;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(
            String receiptNo
    ) {
        this.receiptNo = receiptNo;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(
            String referenceNo
    ) {
        this.referenceNo = referenceNo;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(
            PaymentType paymentType
    ) {
        this.paymentType = paymentType;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(
            LocalDateTime paymentDate
    ) {
        this.paymentDate = paymentDate;
    }
}
