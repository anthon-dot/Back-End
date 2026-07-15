package com.code.back_end.controller;

import com.code.back_end.dto.AINotificationDTO;
import com.code.back_end.service.NotificationAIService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/notifications")
public class AINotificationController {

    private final NotificationAIService notificationAIService;

    public AINotificationController(NotificationAIService notificationAIService) {
        this.notificationAIService = notificationAIService;
    }

    @GetMapping
    public List<AINotificationDTO> getNotifications() {
        return notificationAIService.getNotifications();
    }

    @GetMapping("/unread")
    public List<AINotificationDTO> getUnreadNotifications() {
        return notificationAIService.getUnreadNotifications();
    }

    @PostMapping("/generate")
    public List<AINotificationDTO> generateNotifications() {
        return notificationAIService.generateNotifications();
    }

    @PutMapping("/{id}/read")
    public AINotificationDTO markAsRead(@PathVariable Long id) {
        return notificationAIService.markAsRead(id);
    }

    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable Long id) {
        notificationAIService.delete(id);
    }
}
