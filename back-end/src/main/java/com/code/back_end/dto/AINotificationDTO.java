package com.code.back_end.dto;

import com.code.back_end.entity.Notification;
import com.code.back_end.entity.Stakeholder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AINotificationDTO {

    private Long id;
    private String title;
    private String message;
    private String explanation;
    private String recommendation;
    private String priority;
    private String notificationType;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String stakeholderName;
    private String businessName;
    private String businessType;
    private String relatedRecordType;
    private Long relatedRecordId;
    private List<String> suggestedActions = new ArrayList<>();

    public AINotificationDTO() {
    }

    public AINotificationDTO(Notification notification) {
        Stakeholder stakeholder = notification.getStakeholder();

        this.id = notification.getId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.explanation = notification.getExplanation();
        this.recommendation = notification.getRecommendation();
        this.priority = notification.getPriority();
        this.notificationType = notification.getNotificationType();
        this.isRead = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
        this.relatedRecordType = notification.getRelatedRecordType();
        this.relatedRecordId = notification.getRelatedRecordId();

        if (stakeholder != null) {
            this.businessName = stakeholder.getBusinessName();
            this.businessType = stakeholder.getBusinessType();
            this.stakeholderName = joinName(
                    stakeholder.getFirstName(),
                    stakeholder.getLastName()
            );
        }

        this.suggestedActions = buildSuggestedActions(notification);
    }

    private String joinName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " " +
                (lastName == null ? "" : lastName)).trim();
    }

    private List<String> buildSuggestedActions(Notification notification) {
        String type = notification.getNotificationType() == null
                ? ""
                : notification.getNotificationType();

        if ("OVERDUE_PAYMENT".equalsIgnoreCase(type)) {
            return List.of(
                    "Send payment reminder through official channels",
                    "Review billing history before escalation",
                    "Monitor response within the next collection cycle"
            );
        }

        if ("CONTRACT_EXPIRATION".equalsIgnoreCase(type)) {
            return List.of(
                    "Contact stakeholder for renewal intent",
                    "Prepare renewal requirements",
                    "Flag stall for vacancy planning if renewal is delayed"
            );
        }

        if ("LOW_OCCUPANCY".equalsIgnoreCase(type) ||
                "VACANT_STALL".equalsIgnoreCase(type)) {
            return List.of(
                    "Review vacant stalls for reassignment",
                    "Prioritize qualified pending applicants",
                    "Monitor occupancy movement by stall type"
            );
        }

        return List.of(
                "Review related record",
                "Assign follow-up owner",
                "Update status after action is completed"
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStakeholderName() {
        return stakeholderName;
    }

    public void setStakeholderName(String stakeholderName) {
        this.stakeholderName = stakeholderName;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getRelatedRecordType() {
        return relatedRecordType;
    }

    public void setRelatedRecordType(String relatedRecordType) {
        this.relatedRecordType = relatedRecordType;
    }

    public Long getRelatedRecordId() {
        return relatedRecordId;
    }

    public void setRelatedRecordId(Long relatedRecordId) {
        this.relatedRecordId = relatedRecordId;
    }

    public List<String> getSuggestedActions() {
        return suggestedActions;
    }

    public void setSuggestedActions(List<String> suggestedActions) {
        this.suggestedActions = suggestedActions;
    }
}
