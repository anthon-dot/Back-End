package com.code.back_end.dto;

import com.code.back_end.entity.Notification;

import java.time.LocalDateTime;

public class NotificationDTO {

    private Long id;
    private Long stakeholderId;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public NotificationDTO() {
    }

    public NotificationDTO(Notification notification) {
        this.id = notification.getId();
        this.stakeholderId = notification.getStakeholder() == null
                ? null
                : notification.getStakeholder().getId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.isRead = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getStakeholderId() {
        return stakeholderId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
