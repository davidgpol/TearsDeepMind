package com.tearsdeepmind.domain.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawler_jobs", schema = "crawler")
public class CrawlerJobEntity {

    @Id
    @Column(name = "job_id")
    private String jobId;

    @Column(name = "section")
    private String section;

    @Column(name = "status")
    private String status;

    @Column(name = "start_time")
    private String startTime; // SQLite stores dates as Strings

    @Column(name = "end_time")
    private String endTime;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON String

    public CrawlerJobEntity() {}

    // Getters and Setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
