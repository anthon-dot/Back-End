// =======================================
// BillingService.java
// =======================================
package com.code.back_end.service;

import com.code.back_end.dto.BillingDTO;
import com.code.back_end.entity.Billing;
import com.code.back_end.entity.Contract;

import com.code.back_end.repository.BillingRepository;
import com.code.back_end.repository.ContractRepository;
import com.code.back_end.repository.OccupantRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.security.SecurityService;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingService {

    private static final Logger log =
            LoggerFactory.getLogger(BillingService.class);

    private final BillingRepository billingRepository;

    private final ContractRepository contractRepository;
    private final OccupantRepository occupantRepository;

    private final NotificationService notificationService;
    private final StakeholderRepository stakeholderRepository;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;

    public BillingService(

            BillingRepository billingRepository,

            ContractRepository contractRepository,

            OccupantRepository occupantRepository,

            NotificationService notificationService,

            StakeholderRepository stakeholderRepository,

            SecurityService securityService,

            AuditLogService auditLogService
    ) {

        this.billingRepository =
                billingRepository;

        this.contractRepository =
                contractRepository;

        this.occupantRepository =
                occupantRepository;

        this.notificationService =
                notificationService;

        this.stakeholderRepository =
                stakeholderRepository;

        this.securityService =
                securityService;

        this.auditLogService =
                auditLogService;
    }

    // =====================================
    // GENERATE INITIAL BILLING
    // =====================================
   // =====================================
// GENERATE INITIAL BILLING
// =====================================
@Transactional
public void generateInitialBilling(
        Contract contract
) {

    if (contract == null || contract.getId() == null) {

        log.debug(
                "[Billing] initial billing skipped because contract is null"
        );

        return;
    }

    log.debug(
            "[Billing] generating initial billing for contract {} status {} occupant {}",
            contract.getId(),
            contract.getStatus(),
            contract.getOccupant() == null
                    ? null
                    : contract.getOccupant().getId()
    );

    // =============================
    // ACTIVE ONLY
    // =============================
    if (!"ACTIVE".equalsIgnoreCase(
            contract.getStatus()
    )) {

        log.debug(
                "[Billing] initial billing skipped for inactive contract {}",
                contract.getId()
        );

        return;
    }

    // =============================
    // OCCUPANT REQUIRED
    // =============================
    if (contract.getOccupant() == null) {

        log.debug(
                "[Billing] initial billing skipped because contract {} has no occupant",
                contract.getId()
        );

        return;
    }

    // =============================
    // START DATE REQUIRED
    // =============================
    if (contract.getStartDate() == null) {

        log.debug(
                "[Billing] initial billing skipped because contract {} has no start date",
                contract.getId()
        );

        return;
    }

    String frequency =
            contract.getBillingFrequency();

    String normalizedFrequency =
            frequency == null
                    ? "MONTHLY"
                    : frequency.trim().toUpperCase();

    // =================================
    // USE CONTRACT START DATE
    // =================================
    LocalDate billingDate =
            contract.getStartDate();

    LocalDate today =
            LocalDate.now();

    // =================================
    // MONTHLY
    // =================================
    if ("MONTHLY".equals(normalizedFrequency)) {

        while (!billingDate.isAfter(today)) {

            createBilling(

                    contract,

                    billingDate,

                    "MONTHLY-"
                            + billingDate.getMonthValue()
                            + "-"
                            + billingDate.getYear()
            );

            billingDate =
                    billingDate.plusMonths(1);
        }
    }

    // =================================
    // WEEKLY
    // =================================
    else if ("WEEKLY".equals(normalizedFrequency)) {

        while (!billingDate.isAfter(today)) {

            createBilling(

                    contract,

                    billingDate,

                    "WEEKLY-"
                            + billingDate
            );

            billingDate =
                    billingDate.plusWeeks(1);
        }
    }

    // =================================
    // SEMI MONTHLY / 15 DAYS
    // =================================
    else if (
            "SEMI_MONTHLY".equals(normalizedFrequency)
            ||
            "15_DAYS".equals(normalizedFrequency)
    ) {

        while (!billingDate.isAfter(today)) {

            createBilling(

                    contract,

                    billingDate,

                    "SEMI_MONTHLY-"
                            + billingDate
            );

            billingDate =
                    billingDate.plusDays(15);
        }
    }

    // =================================
    // DEFAULT
    // =================================
    else {

        while (!billingDate.isAfter(today)) {

            createBilling(

                    contract,

                    billingDate,

                    normalizedFrequency
                            + "-"
                            + billingDate
            );

            billingDate =
                    billingDate.plusMonths(1);
        }
    }
}
    // =====================================
    // AUTO GENERATE FUTURE BILLINGS
   // =====================================
// AUTO GENERATE FUTURE BILLINGS
// =====================================
@Scheduled(cron = "0 0 0 * * *")
@Transactional
public void autoGenerateBillings() {

    List<Contract> contracts =
            contractRepository.findAll();

    LocalDate today =
            LocalDate.now();

    for (Contract contract : contracts) {

        // =============================
        // ACTIVE ONLY
        // =============================
        if (!"ACTIVE".equalsIgnoreCase(
                contract.getStatus()
        )) {
            continue;
        }

        // =============================
        // OCCUPANT REQUIRED
        // =============================
        if (contract.getOccupant() == null) {
            continue;
        }

        // =============================
        // CONTRACT EXPIRED
        // =============================
        if (contract.getEndDate() != null
                &&
                contract.getEndDate().isBefore(today)) {

            continue;
        }

        String frequency =
                contract.getBillingFrequency() == null
                        ? "MONTHLY"
                        : contract.getBillingFrequency()
                                .trim()
                                .toUpperCase();

        // =============================
        // GET LATEST BILLING
        // =============================
        List<Billing> existingBillings =
                billingRepository.findByContractIdOrderByDueDateAsc(
                        contract.getId()
                );

        LocalDate nextBillingDate;

        // =============================
        // NO BILLINGS YET
        // =============================
        if (existingBillings.isEmpty()) {

            nextBillingDate =
                    contract.getStartDate();
        }

        // =============================
        // CONTINUE FROM LAST BILLING
        // =============================
        else {

            Billing latestBilling =
                    existingBillings.get(
                            existingBillings.size() - 1
                    );

            nextBillingDate =
                    latestBilling.getDueDate();

            // MONTHLY
            if ("MONTHLY".equals(frequency)) {

                nextBillingDate =
                        nextBillingDate.plusMonths(1);
            }

            // WEEKLY
            else if ("WEEKLY".equals(frequency)) {

                nextBillingDate =
                        nextBillingDate.plusWeeks(1);
            }

            // SEMI MONTHLY
            else if (
                    "SEMI_MONTHLY".equals(frequency)
                    ||
                    "15_DAYS".equals(frequency)
            ) {

                nextBillingDate =
                        nextBillingDate.plusDays(15);
            }

            // DEFAULT
            else {

                nextBillingDate =
                        nextBillingDate.plusMonths(1);
            }
        }

        // =============================
        // GENERATE ONLY IF DUE
        // =============================
        if (!nextBillingDate.isAfter(today)) {

            String billingPeriod;

            // MONTHLY
            if ("MONTHLY".equals(frequency)) {

                billingPeriod =
                        "MONTHLY-"
                        + nextBillingDate.getMonthValue()
                        + "-"
                        + nextBillingDate.getYear();
            }

            // WEEKLY
            else if ("WEEKLY".equals(frequency)) {

                billingPeriod =
                        "WEEKLY-"
                        + nextBillingDate;
            }

            // SEMI MONTHLY
            else if (
                    "SEMI_MONTHLY".equals(frequency)
                    ||
                    "15_DAYS".equals(frequency)
            ) {

                billingPeriod =
                        "SEMI_MONTHLY-"
                        + nextBillingDate;
            }

            // DEFAULT
            else {

                billingPeriod =
                        frequency
                        + "-"
                        + nextBillingDate;
            }

            createBilling(

                    contract,

                    nextBillingDate,

                    billingPeriod
            );
        }
    }
}

    // =====================================
    // CREATE BILLING
    // =====================================
    private void createBilling(

            Contract contract,

            LocalDate dueDate,

            String billingPeriod
    ) {

        // =============================
        // PREVENT DUPLICATES
        // =============================
        boolean exists =

                billingRepository
                        .existsByContractIdAndBillingPeriod(

                                contract.getId(),

                                billingPeriod
                        );

        if (exists) {
            log.debug(
                    "[Billing] duplicate billing skipped for contract {} period {}",
                    contract.getId(),
                    billingPeriod
            );
            return;
        }

        Billing billing =
                new Billing();

        // =============================
        // CONTRACT
        // =============================
        billing.setContract(
                contract
        );

        // =============================
        // OCCUPANT
        // =============================
        billing.setOccupant(
                contract.getOccupant()
        );

        // =============================
        // BILLING NUMBER
        // =============================
        billing.setBillingNo(
                "BILL-"
                + UUID.randomUUID()
        );

        // =============================
        // BILLING PERIOD
        // =============================
        billing.setBillingPeriod(
                billingPeriod
        );

        // =============================
        // COMPUTE AMOUNT
        // =============================
        BigDecimal amount =
                computeAmount(contract);

        billing.setTotalAmount(
                amount
        );

        // =============================
        // APPLY ADVANCE BALANCE
        // =============================
        BigDecimal advance =

                contract.getOccupant()
                        .getAdvanceBalance() == null
                        ? BigDecimal.ZERO
                        : contract.getOccupant()
                                .getAdvanceBalance();

        // NO ADVANCE
        if (advance.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            billing.setPaidAmount(
                    BigDecimal.ZERO
            );

            billing.setBalance(
                    amount
            );

            billing.setStatus(
                    "UNPAID"
            );
        }

        // FULLY COVERED
        else if (advance.compareTo(
                amount
        ) >= 0) {

            billing.setPaidAmount(
                    amount
            );

            billing.setBalance(
                    BigDecimal.ZERO
            );

            billing.setStatus(
                    "PAID"
            );

            contract.getOccupant()
                    .setAdvanceBalance(

                            advance.subtract(
                                    amount
                            )
                    );
        }

        // PARTIAL
        else {

            billing.setPaidAmount(
                    advance
            );

            billing.setBalance(
                    amount.subtract(
                            advance
                    )
            );

            billing.setStatus(
                    "PARTIAL"
            );

            contract.getOccupant()
                    .setAdvanceBalance(
                            BigDecimal.ZERO
                    );
        }

        // =============================
        // DUE DATE
        // =============================
        billing.setDueDate(
                dueDate
        );

        // =============================
        // SAVE
        // =============================
        billingRepository.save(
                billing
        );

        log.debug(
                "[Billing] saved billing {} for contract {} occupant {} period {} amount {} status {}",
                billing.getBillingNo(),
                contract.getId(),
                contract.getOccupant().getId(),
                billingPeriod,
                billing.getTotalAmount(),
                billing.getStatus()
        );

        if (contract.getOccupant() != null) {
            occupantRepository.save(
                    contract.getOccupant()
            );

            if (contract.getOccupant().getStakeholder() != null) {
                contract.getOccupant().getStakeholder()
                        .setAdvanceBalance(
                                contract.getOccupant()
                                        .getAdvanceBalance()
                        );
                stakeholderRepository.save(
                        contract.getOccupant()
                                .getStakeholder()
                );
            }
        }
    }

    // =====================================
    // DUE DATE REMINDERS
    // =====================================
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDueDateReminders() {

        List<Billing> billings =
                billingRepository.findAll();

        for (Billing billing : billings) {

            // SKIP PAID
            if ("PAID".equalsIgnoreCase(
                    billing.getStatus()
            )) {
                continue;
            }

            long daysRemaining =

                    ChronoUnit.DAYS.between(

                            LocalDate.now(),

                            billing.getDueDate()
                    );

            // =================================
            // 3 DAYS BEFORE DUE
            // =================================
            if (daysRemaining == 3) {

                notificationService
                        .createNotification(

                                billing.getOccupant()
                                        .getStakeholder(),

                                "Billing Due Reminder",

                                "Your billing "
                                + billing.getBillingNo()
                                + " is due in 3 days. "
                                + "Remaining balance: ₱"
                                + billing.getBalance()
                        );
            }

            // =================================
            // DUE TODAY
            // =================================
            if (daysRemaining == 0) {

                notificationService
                        .createNotification(

                                billing.getOccupant()
                                        .getStakeholder(),

                                "Billing Due Today",

                                "Your billing "
                                + billing.getBillingNo()
                                + " is due today."
                        );
            }

            // =================================
            // OVERDUE
            // =================================
            if (daysRemaining < 0) {

                notificationService
                        .createNotification(

                                billing.getOccupant()
                                        .getStakeholder(),

                                "Billing Overdue",

                                "Your billing "
                                + billing.getBillingNo()
                                + " is overdue."
                        );
            }
        }
    }

    // =====================================
    // COMPUTE BILL AMOUNT
    // =====================================
    private BigDecimal computeAmount(
            Contract contract
    ) {

        BigDecimal amount =
                contract.getMonthlyRent();

        String frequency =
                contract.getBillingFrequency();

        // WEEKLY
        if ("WEEKLY".equalsIgnoreCase(
                frequency
        )) {

            amount =
                    amount.divide(

                            BigDecimal.valueOf(4),

                            2,

                            RoundingMode.HALF_UP
                    );
        }

        // SEMI MONTHLY
        else if ("SEMI_MONTHLY"
                .equalsIgnoreCase(
                        frequency
                )
                || "15_DAYS".equalsIgnoreCase(
                        frequency
                )) {

            amount =
                    amount.divide(

                            BigDecimal.valueOf(2),

                            2,

                            RoundingMode.HALF_UP
                    );
        }

        return amount;
    }

    // =====================================
    // GET BILLING BY ID
    // =====================================
    public Optional<Billing> getBillingById(
            Long id
    ) {

        if (securityService.canManageOperations()) {
            return billingRepository.findById(
                    id
            );
        }

        securityService.requireVerifiedStakeholderOwnerOrStaff(
                securityService.currentUser().getId()
        );

        return billingRepository
                .findByIdAndOccupant_Stakeholder_User_Id(
                        id,
                        securityService.currentUser()
                                .getId()
                );
    }

    // =====================================
    // DELETE BILLING
    // =====================================
    public void deleteBilling(
            Long id
    ) {

        securityService.requireAdmin();

        Billing billing =

                billingRepository.findById(id)
                        .orElseThrow(() ->

                                new RuntimeException(
                                        "Billing not found"
                                )
                        );

        billingRepository.delete(
                billing
        );

        auditLogService.log(
                "BILLING_DELETED",
                "Billing",
                id,
                "Billing deleted"
        );
    }

    // =====================================
    // GET ALL BILLINGS
    // =====================================
    public List<BillingDTO>
    getAllBillingDTOs() {

        List<Billing> billings;

        if (securityService.canManageOperations()) {
            billings =
                    billingRepository.findAll();
        } else {
            securityService.requireVerifiedStakeholderOwnerOrStaff(
                    securityService.currentUser().getId()
            );

            billings =
                    billingRepository
                            .findByOccupant_Stakeholder_User_IdOrderByDueDateAsc(
                                    securityService.currentUser()
                                            .getId()
                            );
        }

        return mapToDTOList(
                billings
        );
    }

    // =====================================
    // GET BILLINGS BY STAKEHOLDER
    // =====================================
    public List<BillingDTO>
    getBillingByStakeholder(
            Long stakeholderId
    ) {

        var stakeholder =
                stakeholderRepository.findById(stakeholderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Stakeholder not found"
                                )
                        );

        securityService.requireStakeholderOwnerOrStaff(
                stakeholder.getUser().getId()
        );

        securityService.requireVerifiedStakeholderOwnerOrStaff(
                stakeholder.getUser().getId()
        );

        List<Billing> billings =

                billingRepository
                        .findByOccupant_Stakeholder_IdOrderByDueDateAsc(
                                stakeholderId
                        );

        return mapToDTOList(
                billings
        );
    }

    // =====================================
    // MAP ENTITY TO DTO
    // =====================================
    private List<BillingDTO>
    mapToDTOList(
            List<Billing> billings
    ) {

        return billings.stream()
                .map(b -> {

                    BillingDTO dto =
                            new BillingDTO();

                    dto.setId(
                            b.getId()
                    );

                    dto.setBillingNo(
                            b.getBillingNo()
                    );

                    dto.setBillingPeriod(
                            b.getBillingPeriod()
                    );

                    dto.setTotalAmount(
                            b.getTotalAmount()
                    );

                    dto.setPaidAmount(
                            b.getPaidAmount()
                    );

                    dto.setBalance(
                            b.getBalance()
                    );

                    dto.setDueDate(
                            b.getDueDate()
                    );

                    dto.setStatus(
                            b.getStatus()
                    );

                    // CONTRACT
                   if (b.getContract()
        != null) {

    dto.setContractId(

            b.getContract()
                    .getId()
    );

    dto.setBillingFrequency(

            b.getContract()
                    .getBillingFrequency()
    );
}

                    // OCCUPANT
                    if (

                            b.getOccupant() != null
                            &&

                            b.getOccupant()
                                    .getStakeholder()
                                    != null
                    ) {

                        var s =

                                b.getOccupant()
                                        .getStakeholder();

                        dto.setStakeholderId(
                                s.getId()
                        );

                        String fullName =

                                (
                                        (
                                                s.getFirstName()
                                                        != null
                                                        ? s.getFirstName()
                                                        : ""
                                        )
                                        + " "
                                        +
                                        (
                                                s.getLastName()
                                                        != null
                                                        ? s.getLastName()
                                                        : ""
                                        )
                                ).trim();

                        dto.setOccupantName(

                                fullName.isEmpty()
                                ? "Unknown"
                                : fullName
                        );
                    }

                    else {

                        dto.setOccupantName(
                                "Unknown"
                        );
                    }

                    return dto;

                }).toList();
    }
public void sendBillingNotification(Billing billing) {

    securityService.requireSupervisorOrAdmin();

    notificationService.createNotification(

            billing.getOccupant().getStakeholder(),

            "Billing Reminder",

            "Billing " + billing.getBillingNo()
            + " balance: ₱" + billing.getBalance()
    );

    auditLogService.log(
            "BILLING_NOTIFICATION_SENT",
            "Billing",
            billing.getId(),
            "Billing notification sent"
    );
}
public Billing getByBillingNo(
        String billingNo
) {

    if (securityService.canManageOperations()) {
        return billingRepository
                .findByBillingNo(billingNo)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Billing not found"
                        )
                );
    }

    return billingRepository
            .findByBillingNoAndOccupant_Stakeholder_User_Id(
                    billingNo,
                    securityService.currentUser()
                            .getId()
            )
            .filter(billing -> {
                securityService.requireVerifiedStakeholderOwnerOrStaff(
                        securityService.currentUser().getId()
                );
                return true;
            })
            .orElseThrow(() ->
                    new AccessDeniedException(
                            "Billing does not belong to the current tenant"
                    )
            );
}
}
