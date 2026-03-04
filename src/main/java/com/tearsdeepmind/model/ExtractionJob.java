package com.tearsdeepmind.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ExtractionJob {
    private String jobId;
    private String section;
    private int targetDays;
    private java.time.LocalDate targetDate;
    private int totalThreads; // Added missing field
    private JobStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private List<String> pendingUrls = new ArrayList<>();
    private List<String> completedUrls = new ArrayList<>();
    private int failedCount = 0;
    private List<String> errors = new ArrayList<>();
    private Map<String, ThreadTaskStatus> taskDetails = new ConcurrentHashMap<>();

    public ExtractionJob() {}

    public ExtractionJob(String jobId, String section, int targetDays) {
        this.jobId = jobId;
        this.section = section;
        this.targetDays = targetDays;
        this.status = JobStatus.INITIALIZING;
        this.startTime = LocalDateTime.now();
    }

    public ExtractionJob(String jobId, String section, int targetDays, java.time.LocalDate targetDate) {
        this(jobId, section, targetDays);
        this.targetDate = targetDate;
    }

    // --- Core logic helper ---
    public synchronized void markUrlAsCompleted(String url) {
        pendingUrls.remove(url);
        completedUrls.add(url);
        ThreadTaskStatus task = taskDetails.get(url);
        if (task != null) {
            task.setStatus("COMPLETED");
        }
    }

    public synchronized void markUrlAsFailed(String url, String error) {
        ThreadTaskStatus task = taskDetails.computeIfAbsent(url, k -> new ThreadTaskStatus(url));
        task.setRetries(task.getRetries() + 1);
        task.setLastError(error);
        if (task.getRetries() >= 3) {
            task.setStatus("FAILED");
            pendingUrls.remove(url);
            this.failedCount++; 
        } else {
            task.setStatus("RETRYING");
        }
    }

    public int getCompletedCount() {
        return completedUrls.size();
    }

    // Getters and Setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public int getTargetDays() { return targetDays; }
    public void setTargetDays(int targetDays) { this.targetDays = targetDays; }
    public java.time.LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(java.time.LocalDate targetDate) { this.targetDate = targetDate; }
    public int getTotalThreads() { return totalThreads; }
    public void setTotalThreads(int totalThreads) { this.totalThreads = totalThreads; } // Added missing setter
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public List<String> getPendingUrls() { return pendingUrls; }
    public void setPendingUrls(List<String> pendingUrls) { this.pendingUrls = pendingUrls; }
    public List<String> getCompletedUrls() { return completedUrls; }
    public void setCompletedUrls(List<String> completedUrls) { this.completedUrls = completedUrls; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    public Map<String, ThreadTaskStatus> getTaskDetails() { return taskDetails; }
    public void setTaskDetails(Map<String, ThreadTaskStatus> taskDetails) { this.taskDetails = taskDetails; }

    public static class ThreadTaskStatus {
        private String url;
        private String status; // PENDING, PROCESSING, COMPLETED, RETRYING, FAILED
        private int retries;
        private String lastError;

        public ThreadTaskStatus() {}
        public ThreadTaskStatus(String url) {
            this.url = url;
            this.status = "PENDING";
            this.retries = 0;
        }

        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getRetries() { return retries; }
        public void setRetries(int retries) { this.retries = retries; }
        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }
    }
}
