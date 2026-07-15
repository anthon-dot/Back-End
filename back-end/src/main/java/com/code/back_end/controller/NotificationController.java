package com.code.back_end.controller;

import com.code.back_end.dto.AINotificationDTO;
import com.code.back_end.dto.NotificationDTO;
import com.code.back_end.entity.Notification;
import com.code.back_end.repository.NotificationRepository;

import com.code.back_end.service.NotificationService;
import com.code.back_end.service.SmartNotificationService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")

public class NotificationController {

    private final NotificationService
            notificationService;
    private final SmartNotificationService
            smartNotificationService;
    private final NotificationRepository
            notificationRepository;

    public NotificationController(
            NotificationService notificationService,
            SmartNotificationService smartNotificationService,
            NotificationRepository notificationRepository
    ) {

        this.notificationService =
                notificationService;
        this.smartNotificationService =
                smartNotificationService;
        this.notificationRepository =
                notificationRepository;
    }

    // =========================
    // GET ALL NOTIFICATIONS
    // =========================
    @GetMapping
    public List<AINotificationDTO>
    getAllNotifications() {
        return notificationRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AINotificationDTO::new)
                .toList();
    }

    @GetMapping("/stakeholder/{id}")
    public List<NotificationDTO>
    getByStakeholder(
            @PathVariable Long id
    ) {

        return notificationService
                .getByStakeholder(id)
                .stream()
                .map(NotificationDTO::new)
                .toList();
    }

    // =========================
    // MARK AS READ
    // =========================
    @PutMapping("/{id}/read")
    public Notification markAsRead(
            @PathVariable Long id
    ) {

        return notificationService
                .markAsRead(id);
    }

    @GetMapping("/stakeholder/{id}/unread-count")
    public long getUnreadCount(
            @PathVariable Long id
    ) {
        return notificationService
                .getUnreadCount(id);
    }

    @PutMapping("/stakeholder/{id}/read-all")
    public List<NotificationDTO> markAllAsRead(
            @PathVariable Long id
    ) {
        return notificationService
                .markAllAsRead(id)
                .stream()
                .map(NotificationDTO::new)
                .toList();
    }

    @PostMapping("/smart/generate")
    public int generateSmartNotifications() {
        return smartNotificationService
                .generateDailyRecommendations();
    }
}
