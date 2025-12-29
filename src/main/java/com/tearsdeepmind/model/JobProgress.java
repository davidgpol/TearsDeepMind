package com.tearsdeepmind.model;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class JobProgress {
    private String jobId;
    private String status; // INITIALIZING, DISCOVERING, PROCESSING, COMPLETED, FAILED
    private int totalThreads;
    private int completedCount;
    private int failedCount;
    private List<String> errors = new ArrayList<>();
    private Map<String, Integer> retryCount = new ConcurrentHashMap<>();

    public JobProgress(String jobId) {
        this.jobId = jobId;
        this.status = "INITIALIZING";
    }

    // Getters and Setters
    public String getJobId() { return jobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalThreads() { return totalThreads; }
    public void setTotalThreads(int totalThreads) { this.totalThreads = totalThreads; }
    public int getCompletedCount() { return completedCount; }
    public synchronized void incrementCompleted() { this.completedCount++; }
    public int getFailedCount() { return failedCount; }
    public synchronized void incrementFailed(String error) { 
        this.failedCount++; 
        this.errors.add(error);
    }
    public List<String> getErrors() { return errors; }
    
    public int getRetryAttempt(String url) {
        return retryCount.getOrDefault(url, 0);
    }
    
    public void incrementRetry(String url) {
        retryCount.put(url, retryCount.getOrDefault(url, 0) + 1);
    }
}