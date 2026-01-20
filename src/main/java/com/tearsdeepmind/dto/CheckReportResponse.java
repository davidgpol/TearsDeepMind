package com.tearsdeepmind.dto;

import java.time.LocalDateTime;

public class CheckReportResponse {
    private String section;
    private String targetDate;
    private boolean exists;
    private String title;
    private String url;
    private LocalDateTime timestamp;

    public CheckReportResponse(String section, String targetDate, boolean exists, String title, String url) {
        this.section = section;
        this.targetDate = targetDate;
        this.exists = exists;
        this.title = title;
        this.url = url;
        this.timestamp = LocalDateTime.now();
    }

    public static CheckReportResponse notFound(String section, String targetDate) {
        return new CheckReportResponse(section, targetDate, false, null, null);
    }

    // Getters
    public String getSection() { return section; }
    public String getTargetDate() { return targetDate; }
    public boolean isExists() { return exists; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
