package com.code.back_end.service;

import com.code.back_end.entity.Billing;
import com.code.back_end.entity.Contract;
import com.code.back_end.entity.Notification;
import com.code.back_end.entity.Stakeholder;
import com.code.back_end.repository.BillingRepository;
import com.code.back_end.repository.ContractRepository;
import com.code.back_end.repository.NotificationRepository;
import com.code.back_end.repository.StakeholderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SmartNotificationService {

    private final BillingRepository billingRepository;
    private final ContractRepository contractRepository;
    private final StakeholderRepository stakeholderRepository;
    private final NotificationRepository notificationRepository;

    public SmartNotificationService(
            BillingRepository billingRepository,
            ContractRepository contractRepository,
            StakeholderRepository stakeholderRepository,
            NotificationRepository notificationRepository
    ) {
        this.billingRepository = billingRepository;
        this.contractRepository = contractRepository;
        this.stakeholderRepository = stakeholderRepository;
        this.notificationRepository = notificationRepository;
    }

    public int generateDailyRecommendations() {
        int created = 0;
        created += generateBillingNotifications();
        created += generateContractNotifications();
        created += generateApplicationNotifications();
        return created;
    }

    private int generateBillingNotifications() {
        int created = 0;
        LocalDate today = LocalDate.now();

        for (Billing billing : billingRepository.findAll()) {
            if (billing.getOccupant() == null ||
                    billing.getOccupant().getStakeholder() == null ||
                    billing.getDueDate() == null ||
                    balance(billing).compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Stakeholder stakeholder = billing.getOccupant().getStakeholder();

            if (billing.getDueDate().isBefore(today)) {
                created += createOncePerDay(
                        stakeholder,
                        "Overdue payment",
                        "Your billing " + billing.getBillingNo() +
                                " is overdue with a remaining balance of PHP " +
                                balance(billing) + "."
                );
            } else if (!billing.getDueDate().isAfter(today.plusDays(3))) {
                created += createOncePerDay(
                        stakeholder,
                        "Payment due soon",
                        "Reminder: your payment for billing " + billing.getBillingNo() +
                                " is due on " + billing.getDueDate() + "."
                );
            }
        }

        return created;
    }

    private int generateContractNotifications() {
        int created = 0;
        LocalDate threshold = LocalDate.now().plusDays(30);

        for (Contract contract : contractRepository.findAll()) {
            if (contract.getOccupant() == null ||
                    contract.getOccupant().getStakeholder() == null ||
                    contract.getEndDate() == null ||
                    contract.getEndDate().isAfter(threshold) ||
                    !"ACTIVE".equalsIgnoreCase(nullSafe(contract.getStatus()))) {
                continue;
            }

            created += createOncePerDay(
                    contract.getOccupant().getStakeholder(),
                    "Contract expiring soon",
                    "Your contract " + contract.getContractNo() +
                            " will expire on " + contract.getEndDate() + "."
            );
        }

        return created;
    }

    private int generateApplicationNotifications() {
        int created = 0;

        List<Stakeholder> stakeholders = stakeholderRepository.findAll();

        for (Stakeholder stakeholder : stakeholders) {
            if ("PENDING".equalsIgnoreCase(nullSafe(stakeholder.getApplicationStatus())) &&
                    !Boolean.TRUE.equals(stakeholder.getIsArchived())) {
                created += createOncePerDay(
                        stakeholder,
                        "Application pending",
                        "Your application is still pending review. We will notify you once it moves forward."
                );
            }
        }

        return created;
    }

    private int createOncePerDay(
            Stakeholder stakeholder,
            String title,
            String message
    ) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        boolean exists = notificationRepository
                .existsByStakeholderIdAndTitleAndCreatedAtBetween(
                        stakeholder.getId(),
                        title,
                        start,
                        end
                );

        if (exists) {
            return 0;
        }

        Notification notification = new Notification();
        notification.setStakeholder(stakeholder);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);

        return 1;
    }

    private BigDecimal balance(Billing billing) {
        return billing.getBalance() == null
                ? BigDecimal.ZERO
                : billing.getBalance();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
