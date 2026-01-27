package com.tearsdeepmind.model;

/**
 * Record for real-time monitoring events.
 */
public record CrawlerEvent(String jobId, String type, String title, int current, int total, String message) {}
