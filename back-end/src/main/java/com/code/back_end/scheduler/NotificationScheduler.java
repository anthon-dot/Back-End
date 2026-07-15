package com.code.back_end.scheduler;

import com.code.back_end.service.SmartNotificationService;
import com.code.back_end.service.NotificationAIService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    private final SmartNotificationService smartNotificationService;
    private final NotificationAIService notificationAIService;

    public NotificationScheduler(
            SmartNotificationService smartNotificationService,
            NotificationAIService notificationAIService
    ) {
        this.smartNotificationService = smartNotificationService;
        this.notificationAIService = notificationAIService;
    }

    @Scheduled(cron = "${ai.notifications.schedule:0 0 7 * * *}")
    public void generateDailyNotifications() {
        smartNotificationService.generateDailyRecommendations();
        notificationAIService.generateNotifications();
    }
}
