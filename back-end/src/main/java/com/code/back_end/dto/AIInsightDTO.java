package com.code.back_end.dto;

public class AIInsightDTO {

    private String title;
    private String message;
    private String severity;

    public AIInsightDTO() {
    }

    public AIInsightDTO(
            String title,
            String message,
            String severity
    ) {
        this.title = title;
        this.message = message;
        this.severity = severity;
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

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
