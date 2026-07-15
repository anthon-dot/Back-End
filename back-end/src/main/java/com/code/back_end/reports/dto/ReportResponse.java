package com.code.back_end.reports.dto;

import com.code.back_end.reports.Report;

import java.time.LocalDateTime;

public class ReportResponse {

    private Long id;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdDate;
    private String supervisorName;

    public ReportResponse(Report report) {
        this.id = report.getId();
        this.title = report.getTitle();
        this.description = report.getDescription();
        this.status = report.getStatus();
        this.createdDate = report.getCreatedDate();
        this.supervisorName = report.getSupervisorName();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getSupervisorName() {
        return supervisorName;
    }
}
