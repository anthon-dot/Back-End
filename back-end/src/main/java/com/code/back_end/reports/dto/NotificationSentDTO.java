package com.code.back_end.reports.dto;

import java.time.LocalDateTime;

public class NotificationSentDTO {
    private String title;
    private LocalDateTime createdAt;
    private String businessName;

    public NotificationSentDTO(String title, LocalDateTime createdAt, String businessName) {
        this.title = title;
        this.createdAt = createdAt;
        this.businessName = businessName;
    }

    public String getTitle() { return title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getBusinessName() { return businessName; }
}
