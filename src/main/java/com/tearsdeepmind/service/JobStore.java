package com.tearsdeepmind.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tearsdeepmind.domain.model.CrawlerJobEntity;
import com.tearsdeepmind.model.ExtractionJob;
import com.tearsdeepmind.model.JobStatus;
import com.tearsdeepmind.repository.CrawlerJobRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class JobStore {
    private static final Logger logger = LogManager.getLogger(JobStore.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ConcurrentHashMap<String, ExtractionJob> activeJobs = new ConcurrentHashMap<>();
    
    private final CrawlerJobRepository repository;

    public JobStore(CrawlerJobRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        // Load active or recently interrupted jobs from DB could be implemented here
        // For now, we start fresh memory cache, but history remains in DB
        logger.info("JobStore initialized with SQLite persistence.");
    }

    @Transactional
    public void saveJob(ExtractionJob job) {
        // 1. Update In-Memory Cache (Fast Read)
        activeJobs.put(job.getJobId(), job);
        
        // 2. Persist to SQLite (Durable Write)
        try {
            CrawlerJobEntity entity = mapToEntity(job);
            repository.save(entity);
        } catch (Exception e) {
            logger.error("Failed to persist job status to DB: {}", job.getJobId(), e);
        }
    }

    public ExtractionJob getJob(String jobId) {
        // 1. Try Cache
        if (activeJobs.containsKey(jobId)) {
            return activeJobs.get(jobId);
        }
        
        // 2. Try DB (History)
        return repository.findById(jobId)
                .map(this::mapFromEntity)
                .map(job -> {
                    activeJobs.put(jobId, job); // Re-hydrate cache
                    return job;
                })
                .orElse(null);
    }

    public Collection<ExtractionJob> getAllActive() {
        return activeJobs.values();
    }
    
    // --- Mappers ---
    
    private CrawlerJobEntity mapToEntity(ExtractionJob job) throws JsonProcessingException {
        CrawlerJobEntity entity = new CrawlerJobEntity();
        entity.setJobId(job.getJobId());
        entity.setSection(job.getSection());
        entity.setStatus(job.getStatus().name());
        entity.setStartTime(job.getStartTime() != null ? job.getStartTime().toString() : null);
        entity.setEndTime(job.getEndTime() != null ? job.getEndTime().toString() : null);
        
        // Serialize complex details
        JobDetails details = new JobDetails();
        details.pendingUrls = job.getPendingUrls();
        details.completedUrls = job.getCompletedUrls();
        details.failedCount = job.getFailedCount();
        details.errors = job.getErrors();
        details.taskDetails = job.getTaskDetails();
        details.targetDays = job.getTargetDays(); // Also persist config
        details.totalThreads = job.getTotalThreads();
        
        entity.setDetails(mapper.writeValueAsString(details));
        return entity;
    }
    
    private ExtractionJob mapFromEntity(CrawlerJobEntity entity) {
        ExtractionJob job = new ExtractionJob();
        job.setJobId(entity.getJobId());
        job.setSection(entity.getSection());
        try {
            job.setStatus(JobStatus.valueOf(entity.getStatus()));
        } catch (IllegalArgumentException e) {
            job.setStatus(JobStatus.FAILED);
        }
        if (entity.getStartTime() != null) job.setStartTime(LocalDateTime.parse(entity.getStartTime()));
        if (entity.getEndTime() != null) job.setEndTime(LocalDateTime.parse(entity.getEndTime()));
        
        try {
            if (entity.getDetails() != null) {
                JobDetails details = mapper.readValue(entity.getDetails(), JobDetails.class);
                job.setPendingUrls(details.pendingUrls);
                job.setCompletedUrls(details.completedUrls);
                job.setFailedCount(details.failedCount);
                job.setErrors(details.errors);
                job.setTaskDetails(details.taskDetails);
                job.setTargetDays(details.targetDays);
                job.setTotalThreads(details.totalThreads);
            }
        } catch (IOException e) {
            logger.error("Error deserializing job details for {}", entity.getJobId(), e);
        }
        return job;
    }
    
    // Internal DTO for JSON serialization
    private static class JobDetails {
        public int targetDays;
        public int totalThreads;
        public java.util.List<String> pendingUrls;
        public java.util.List<String> completedUrls;
        public int failedCount;
        public java.util.List<String> errors;
        public java.util.Map<String, ExtractionJob.ThreadTaskStatus> taskDetails;
    }
}

