package com.code.back_end.repository;

import com.code.back_end.entity.Notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification>
    findAllByOrderByCreatedAtDesc();

    List<Notification>
    findByStakeholderIdOrderByCreatedAtDesc(
            Long stakeholderId
    );

    List<Notification>
    findByAiGeneratedTrueOrderByCreatedAtDesc();

    List<Notification>
    findByAiGeneratedTrueAndIsReadFalseOrderByCreatedAtDesc();

    boolean existsByStakeholderIdAndTitleAndCreatedAtBetween(
            Long stakeholderId,
            String title,
            LocalDateTime start,
            LocalDateTime end
    );

    boolean existsByAiGeneratedTrueAndNotificationTypeAndRelatedRecordIdAndCreatedAtBetween(
            String notificationType,
            Long relatedRecordId,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByStakeholderIdAndIsReadFalse(
            Long stakeholderId
    );

    List<Notification>
    findByStakeholder_User_IdOrderByCreatedAtDesc(
            Long userId
    );

    Optional<Notification>
    findByIdAndStakeholder_User_Id(
            Long id,
            Long userId
    );

    long countByStakeholder_User_IdAndIsReadFalse(
            Long userId
    );
}
