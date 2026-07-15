package com.code.back_end.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "archive_records")
public class ArchiveRecord {

    @Id
    @GeneratedValue(strategy =
            GenerationType.IDENTITY)
    private Long id;

    private String entityName;

    private Long entityId;

    @ManyToOne
    @JoinColumn(name = "archived_by")
    private User archivedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private LocalDateTime archivedAt =
            LocalDateTime.now();
}