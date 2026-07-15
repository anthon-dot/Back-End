package com.code.back_end.service;

import com.code.back_end.entity.Notification;
import com.code.back_end.entity.Stakeholder;

import com.code.back_end.repository.NotificationRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.security.SecurityService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository
            notificationRepository;
    private final StakeholderRepository stakeholderRepository;
    private final SecurityService securityService;

    public NotificationService(
            NotificationRepository notificationRepository,
            StakeholderRepository stakeholderRepository,
            SecurityService securityService
    ) {

        this.notificationRepository =
                notificationRepository;
        this.stakeholderRepository =
                stakeholderRepository;
        this.securityService =
                securityService;
    }

    // =========================
    // CREATE NOTIFICATION
    // =========================
    public Notification createNotification(
            Stakeholder stakeholder,
            String title,
            String message
    ) {

        Notification notification =
                new Notification();

        notification.setStakeholder(
                stakeholder
        );

        notification.setTitle(
                title
        );

        notification.setMessage(
                message
        );

        return notificationRepository.save(
                notification
        );
    }

    // =========================
    // GET BY STAKEHOLDER
    // =========================
    public List<Notification>
    getByStakeholder(Long stakeholderId) {

        Stakeholder stakeholder =
                stakeholderRepository.findById(stakeholderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Stakeholder not found"
                                )
                        );

        securityService.requireStakeholderOwnerOrStaff(
                stakeholder.getUser().getId()
        );

        return notificationRepository
                .findByStakeholderIdOrderByCreatedAtDesc(
                        stakeholderId
                );
    }

    // =========================
    // MARK AS READ
    // =========================
    public Notification markAsRead(
            Long id
    ) {

        Notification notification;

        if (securityService.canManageOperations()) {
            notification =
                    notificationRepository.findById(id)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Notification not found"
                                    )
                            );
        } else {
            notification =
                    notificationRepository
                            .findByIdAndStakeholder_User_Id(
                                    id,
                                    securityService.currentUser()
                                            .getId()
                            )
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "Notification does not belong to the current tenant"
                                    )
                            );
        }

        notification.setIsRead(true);

        return notificationRepository.save(
                notification
        );
    }

    public long getUnreadCount(Long stakeholderId) {
        Stakeholder stakeholder =
                stakeholderRepository.findById(stakeholderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Stakeholder not found"
                                )
                        );

        securityService.requireStakeholderOwnerOrStaff(
                stakeholder.getUser().getId()
        );

        return notificationRepository
                .countByStakeholderIdAndIsReadFalse(
                        stakeholderId
                );
    }

    public List<Notification> markAllAsRead(
            Long stakeholderId
    ) {
        List<Notification> notifications =
                getByStakeholder(stakeholderId);

        for (Notification notification : notifications) {
            notification.setIsRead(true);
        }

        return notificationRepository.saveAll(
                notifications
        );
    }
}
