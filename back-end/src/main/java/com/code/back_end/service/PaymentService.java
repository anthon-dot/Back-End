package com.code.back_end.service;

import com.code.back_end.entity.Billing;
import com.code.back_end.entity.Contract;
import com.code.back_end.entity.Occupant;
import com.code.back_end.entity.Payment;
import com.code.back_end.entity.PaymentType;
import com.code.back_end.entity.Stakeholder;

import com.code.back_end.repository.BillingRepository;
import com.code.back_end.repository.BusinessApplicationRepository;
import com.code.back_end.repository.ContractRepository;
import com.code.back_end.repository.OccupantRepository;
import com.code.back_end.repository.PaymentRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.security.SecurityService;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    private final BillingRepository billingRepository;

    private final StakeholderRepository stakeholderRepository;

    private final NotificationService notificationService;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;
    private final BusinessApplicationRepository applicationRepository;
    private final ContractRepository contractRepository;
    private final OccupantRepository occupantRepository;
    private final BillingService billingService;

    public PaymentService(
            PaymentRepository paymentRepository,
            BillingRepository billingRepository,
            StakeholderRepository stakeholderRepository,
            NotificationService notificationService,
            SecurityService securityService,
            AuditLogService auditLogService,
            BusinessApplicationRepository applicationRepository,
            ContractRepository contractRepository,
            OccupantRepository occupantRepository,
            BillingService billingService
    ) {

        this.paymentRepository =
                paymentRepository;

        this.billingRepository =
                billingRepository;

        this.stakeholderRepository =
                stakeholderRepository;

        this.notificationService =
                notificationService;

        this.securityService =
                securityService;

        this.auditLogService =
                auditLogService;

        this.applicationRepository =
                applicationRepository;

        this.contractRepository =
                contractRepository;

        this.occupantRepository =
                occupantRepository;

        this.billingService =
                billingService;
    }

    // =========================
    // GET ALL PAYMENTS
    // =========================
    public List<Payment> getAll() {

        if (securityService.canManageOperations()) {
            return paymentRepository.findAll();
        }

        return paymentRepository
                .findByStakeholder_User_Id(
                        securityService.currentUser()
                                .getId()
                );
    }

    public Payment getById(Long id) {

        if (securityService.canManageOperations()) {
            return paymentRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Payment not found"
                            )
                    );
        }

        return paymentRepository
                .findByIdAndStakeholder_User_Id(
                        id,
                        securityService.currentUser()
                                .getId()
                )
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Payment does not belong to the current tenant"
                        )
                );
    }

    // =========================
    // CREATE PAYMENT
    // =========================
    @Transactional
    public Payment createPayment(
            Payment payment
    ) {

        // =========================
        // VALIDATE PAYMENT TYPE
        // =========================
        if (
                payment.getPaymentType()
                        == null
        ) {

            throw new RuntimeException(
                    "Payment type is required"
            );
        }

        // =========================
        // VALIDATE AMOUNT
        // =========================
        if (
                payment.getAmount() == null
                        ||
                        payment.getAmount()
                                .compareTo(
                                        BigDecimal.ZERO
                                ) <= 0
        ) {

            throw new RuntimeException(
                    "Valid payment amount is required"
            );
        }

        // =========================
        // GET STAKEHOLDER
        // =========================
        Stakeholder stakeholder =
                stakeholderRepository.findById(
                        payment.getStakeholder().getId()
                ).orElseThrow(() ->

                        new RuntimeException(
                                "Stakeholder not found"
                        )
                );

        if (payment.getPaymentType() == PaymentType.APPLICATION_FEE
                || payment.getPaymentType() == PaymentType.BUSINESS_PERMIT_PAYMENT) {
            securityService.requireTreasurerOrAdmin();
        } else {
            securityService.requireStakeholderOwnerOrStaff(
                    stakeholder.getUser().getId()
            );
        }

        payment.setStakeholder(
                stakeholder
        );

        log.debug(
                "[Payment] saving payment for stakeholder {} type {} amount {} rentCycle {} billing {}",
                stakeholder.getId(),
                payment.getPaymentType(),
                payment.getAmount(),
                payment.getRentCycle(),
                payment.getBilling() == null ? null : payment.getBilling().getId()
        );

        // =========================
        // AUTO RECEIPT NUMBER
        // =========================
        payment.setReceiptNo(

                "RCPT-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
        );

        // =========================
        // AUTO DATE
        // =========================
        payment.setPaymentDate(
                LocalDateTime.now()
        );

        // =====================================================
        // RENT PAYMENT
        // =====================================================
        if (
                payment.getPaymentType()
                        == PaymentType.RENT_PAYMENT
        ) {

            // =========================
            // VALIDATE BILLING
            // =========================
            if (
                    payment.getBilling()
                            == null
            ) {

                throw new RuntimeException(

                        "Billing is required "
                                + "for rent payment"
                );
            }

            Billing billing =
                    billingRepository.findById(

                            payment.getBilling()
                                    .getId()

                    ).orElseThrow(() ->

                            new RuntimeException(
                                    "Billing not found"
                            )
                    );

            if (
                    billing.getOccupant() == null ||
                    billing.getOccupant().getStakeholder() == null ||
                    !billing.getOccupant().getStakeholder().getId()
                            .equals(stakeholder.getId())
            ) {
                throw new AccessDeniedException(
                        "Billing does not belong to the selected stakeholder"
                );
            }

            if (payment.getRentCycle() == null
                    && billing.getContract() != null) {
                payment.setRentCycle(
                        billing.getContract()
                                .getBillingFrequency()
                );
            }

            if (payment.getRentCycle() == null
                    || payment.getRentCycle().trim().isEmpty()) {
                throw new RuntimeException(
                        "Rent cycle is required"
                );
            }

            payment.setBilling(
                    billing
            );

            BigDecimal paymentAmount =
                    payment.getAmount();

            BigDecimal currentBalance =
                    billing.getBalance();

            // =========================
            // OVERPAYMENT
            // =========================
            if (
                    paymentAmount.compareTo(
                            currentBalance
                    ) > 0
            ) {

                BigDecimal excess =
                        paymentAmount.subtract(
                                currentBalance
                        );

                billing.setPaidAmount(
                        billing.getTotalAmount()
                );

                billing.setBalance(
                        BigDecimal.ZERO
                );

                billing.setStatus(
                        "PAID"
                );

                BigDecimal currentAdvance =

                        stakeholder.getAdvanceBalance()
                                == null

                                ? BigDecimal.ZERO

                                : stakeholder.getAdvanceBalance();

                stakeholder.setAdvanceBalance(

                        currentAdvance.add(
                                excess
                        )
                );

                notificationService
                        .createNotification(

                                stakeholder,

                                "Advance Payment Added",

                                "You overpaid ₱"
                                        + excess
                                        + ". Excess amount "
                                        + "added to advance balance."
                        );
            }

            // =========================
            // EXACT PAYMENT
            // =========================
            else if (
                    paymentAmount.compareTo(
                            currentBalance
                    ) == 0
            ) {

                billing.setPaidAmount(
                        billing.getTotalAmount()
                );

                billing.setBalance(
                        BigDecimal.ZERO
                );

                billing.setStatus(
                        "PAID"
                );

                notificationService
                        .createNotification(

                                stakeholder,

                                "Billing Fully Paid",

                                "Your billing "
                                        + billing.getBillingNo()
                                        + " has been fully paid."
                        );
            }

            // =========================
            // PARTIAL PAYMENT
            // =========================
            else {

                BigDecimal paidAmount =

                        billing.getPaidAmount()
                                == null

                                ? BigDecimal.ZERO

                                : billing.getPaidAmount();

                BigDecimal newPaidAmount =
                        paidAmount.add(
                                paymentAmount
                        );

                billing.setPaidAmount(
                        newPaidAmount
                );

                BigDecimal newBalance =
                        billing.getTotalAmount()
                                .subtract(
                                        newPaidAmount
                                );

                billing.setBalance(
                        newBalance
                );

                billing.setStatus(
                        "PARTIAL"
                );

                notificationService
                        .createNotification(

                                stakeholder,

                                "Partial Payment Received",

                                "Payment of ₱"
                                        + paymentAmount
                                        + " received for billing "
                                        + billing.getBillingNo()
                                        + ". Remaining balance: ₱"
                                        + newBalance
                        );
            }

            billingRepository.save(
                    billing
            );
        }

        // =====================================================
        // ADVANCE PAYMENT
        // =====================================================
        else if (
                payment.getPaymentType()
                        == PaymentType.ADVANCE_PAYMENT
        ) {

            if (
                    payment.getTotalAdvanceAmount()
                            == null
            ) {

                throw new RuntimeException(

                        "Total advance amount is required"
                );
            }

            stakeholder.setTotalAdvanceAmount(
                    payment.getTotalAdvanceAmount()
            );

            BigDecimal currentAdvance =

                    stakeholder.getAdvanceBalance()
                            == null

                            ? BigDecimal.ZERO

                            : stakeholder.getAdvanceBalance();

            BigDecimal newAdvanceBalance =
                    currentAdvance.add(
                            payment.getAmount()
                    );

            stakeholder.setAdvanceBalance(
                    newAdvanceBalance
            );
            stakeholder.setAdvancePaymentAmount(
                    newAdvanceBalance
            );

            if (
                    newAdvanceBalance.compareTo(

                            stakeholder.getTotalAdvanceAmount()

                    ) >= 0
            ) {

                stakeholder.setAdvancePayment(
                        true
                );
                stakeholder.setAdvancePaymentPaid(true);
                stakeholder.setAdvancePaymentCompleted(true);
                stakeholder.setAdvancePaymentDate(LocalDate.now());
                stakeholder.setOnboardingStatus(
                        "FOR_APPROVAL"
                );

            } else {

                stakeholder.setAdvancePayment(
                        false
                );
                stakeholder.setAdvancePaymentPaid(false);
                stakeholder.setAdvancePaymentCompleted(false);
                stakeholder.setOnboardingStatus("PAYMENT_PENDING");
            }

            stakeholderRepository.save(
                    stakeholder
            );
            syncApplicationPayment(stakeholder);

            notificationService
                    .createNotification(

                            stakeholder,

                            "Advance Payment Added",

                            "₱"
                                    + payment.getAmount()
                                    + " added to your advance balance."
                    );
        }

        // =====================================================
        // APPLICATION FORM
        // =====================================================
       else if (
        payment.getPaymentType() == PaymentType.APPLICATION_FORM
        || payment.getPaymentType() == PaymentType.APPLICATION_FEE
        || payment.getPaymentType() == PaymentType.BUSINESS_PERMIT_PAYMENT
) {

    log.info(
            "Business permit/application fee payment detected for stakeholder {} amount {}",
            stakeholder.getId(),
            payment.getAmount()
    );

    verifyStakeholderAfterApplicantFee(
            stakeholder,
            payment.getAmount()
    );

    syncApplicationPayment(stakeholder);
    activateOccupantAndBilling(stakeholder);

    notificationService.createNotification(
            stakeholder,
            "Business Permit Payment Confirmed",
            "Your business permit payment has been confirmed. Your stakeholder account is now active."
    );
}
        Payment saved =
                paymentRepository.save(
                payment
        );

        log.debug(
                "[Payment] saved payment {} stakeholder {} type {} amount {} rentCycle {}",
                saved.getId(),
                stakeholder.getId(),
                saved.getPaymentType(),
                saved.getAmount(),
                saved.getRentCycle()
        );

        auditLogService.log(
                "PAYMENT_RECORDED",
                "Payment",
                saved.getId(),
                "Payment recorded for stakeholder " + stakeholder.getId()
        );

        return saved;
    }

    private void activateOccupantAndBilling(
            Stakeholder stakeholder
    ) {
        if (!Boolean.TRUE.equals(stakeholder.getVerifiedTenant())) {
            return;
        }

        Occupant occupant =
                occupantRepository.findByStakeholder_IdAndIsArchivedFalse(
                        stakeholder.getId()
                ).orElse(null);

        if (occupant == null) {
            log.debug(
                    "[Payment] billing generation skipped because stakeholder {} has no occupant",
                    stakeholder.getId()
            );
            return;
        }

        Contract contract =
                contractRepository.findFirstByOccupant_Stakeholder_IdOrderByCreatedAtDesc(
                        stakeholder.getId()
                ).orElse(null);

        if (contract == null) {
            log.debug(
                    "[Payment] billing generation skipped because stakeholder {} has no contract",
                    stakeholder.getId()
            );
            return;
        }

        occupant.setStatus("ACTIVE");

        if (occupant.getOccupancyDate() == null) {
            occupant.setOccupancyDate(LocalDate.now());
        }

        occupant.setContractId(contract.getId());
        occupant.setAdvanceBalance(
                stakeholder.getAdvanceBalance() == null
                        ? BigDecimal.ZERO
                        : stakeholder.getAdvanceBalance()
        );

        occupantRepository.save(occupant);

        log.debug(
                "[Payment] activated occupant {} stakeholder {} contract {} advanceBalance {} occupancyDate {}",
                occupant.getId(),
                stakeholder.getId(),
                contract.getId(),
                occupant.getAdvanceBalance(),
                occupant.getOccupancyDate()
        );

        billingService.generateInitialBilling(contract);
    }

    private void syncApplicationPayment(
            Stakeholder stakeholder
    ) {
        if (stakeholder.getUser() == null || stakeholder.getUser().getId() == null) {
            return;
        }

        applicationRepository.findByUser_Id(stakeholder.getUser().getId())
                .ifPresent(application -> {
                    application.setAdvancePaymentPaid(stakeholder.getAdvancePaymentPaid());
                    application.setAdvancePaymentCompleted(stakeholder.getAdvancePaymentCompleted());
                    application.setAdvancePaymentAmount(stakeholder.getAdvancePaymentAmount());
                    application.setAdvanceBalance(stakeholder.getAdvanceBalance());
                    application.setTotalAdvanceAmount(stakeholder.getTotalAdvanceAmount());
                    application.setAdvancePaymentDate(stakeholder.getAdvancePaymentDate());
                    application.setApplicationStatus(stakeholder.getApplicationStatus());
                    application.setOnboardingStatus(stakeholder.getOnboardingStatus());
                    application.setApplicantFeePaid(stakeholder.getApplicantFeePaid());
                    application.setApplicantFeeAmount(stakeholder.getApplicantFeeAmount());
                    application.setApplicantFeeDate(stakeholder.getApplicantFeeDate());
                    application.setVerifiedApplication(stakeholder.getVerified());
                    applicationRepository.save(application);
                });
    }

    private void verifyStakeholderAfterApplicantFee(
            Stakeholder stakeholder,
            BigDecimal amount
    ) {
        LocalDate feeDate = LocalDate.now();
        LocalDateTime verificationDate = LocalDateTime.now();

        stakeholder.setApplicantFeePaid(true);
        stakeholder.setVerifiedTenant(true);
        stakeholder.setVerifiedStakeholder(true);
        stakeholder.setApplicantFeeAmount(amount);
        stakeholder.setApplicantFeeDate(feeDate);
        stakeholder.setVerified(true);
        stakeholder.setVerificationDate(verificationDate);

        if (
                "APPROVED".equals(stakeholder.getFinalStatus())
                        || "PENDING_BUSINESS_PERMIT_PAYMENT".equals(stakeholder.getApplicationStatus())
                        || "FULLY_APPROVED".equals(stakeholder.getApplicationStatus())
                        || "APPROVED".equals(stakeholder.getApplicationStatus())
        ) {
            stakeholder.setApplicationStatus("COMPLETED");
            stakeholder.setOnboardingStatus("APPROVED");
            stakeholder.setTreasurerPaid(true);
        }

        stakeholderRepository.save(stakeholder);
        stakeholderRepository.markApplicantFeePaid(
                stakeholder.getId(),
                amount,
                feeDate,
                verificationDate
        );
        log.info(
                "Stakeholder {} verified after applicant fee: applicantFeePaid={}, verified={}, verifiedStakeholder={}, verifiedTenant={}",
                stakeholder.getId(),
                stakeholder.getApplicantFeePaid(),
                stakeholder.getVerified(),
                stakeholder.getVerifiedStakeholder(),
                stakeholder.getVerifiedTenant()
        );
    }
}
