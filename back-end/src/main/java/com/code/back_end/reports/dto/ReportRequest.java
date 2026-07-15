package com.code.back_end.reports.dto;

import jakarta.validation.constraints.NotBlank;

public class ReportRequest {

    @NotBlank(message = "Report title is required")
    private String title;

    private String description;

    @NotBlank(message = "Report status is required")
    private String status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
