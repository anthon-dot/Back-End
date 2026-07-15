package com.code.back_end.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {

                @Index(
                        name = "idx_notification_stakeholder",
                        columnList = "stakeholder_id"
                ),

                @Index(
                        name = "idx_notification_created_at",
                        columnList = "created_at"
                )
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy =
            GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stakeholder_id")
    private Stakeholder stakeholder;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(length = 30)
    private String priority = "LOW";

    private String notificationType;

    private String relatedRecordType;

    private Long relatedRecordId;

    private Boolean aiGenerated = false;

    private Boolean isRead = false;

    private LocalDateTime createdAt =
            LocalDateTime.now();

    // =========================
    // GETTERS & SETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Stakeholder getStakeholder() {
        return stakeholder;
    }

    public void setStakeholder(
            Stakeholder stakeholder
    ) {
        this.stakeholder = stakeholder;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(
            String title
    ) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(
            String message
    ) {
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

    public Boolean getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(
            Boolean isRead
    ) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}
