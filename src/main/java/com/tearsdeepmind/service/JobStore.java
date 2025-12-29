package com.tearsdeepmind.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tearsdeepmind.model.ExtractionJob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobStore {
    private static final Logger logger = LogManager.getLogger(JobStore.class);
    private final Path jobsDir = Paths.get("TearsDeepMind", "TearsMind", "jobs");
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ConcurrentHashMap<String, ExtractionJob> activeJobs = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(jobsDir);
    }

    public void saveJob(ExtractionJob job) {
        activeJobs.put(job.getJobId(), job);
        try {
            File file = jobsDir.resolve(job.getJobId() + ".json").toFile();
            mapper.writeValue(file, job);
        } catch (IOException e) {
            logger.error("Failed to persist job status: {}", job.getJobId(), e);
        }
    }

    public ExtractionJob getJob(String jobId) {
        if (activeJobs.containsKey(jobId)) {
            return activeJobs.get(jobId);
        }
        
        File file = jobsDir.resolve(jobId + ".json").toFile();
        if (file.exists()) {
            try {
                ExtractionJob job = mapper.readValue(file, ExtractionJob.class);
                activeJobs.put(jobId, job);
                return job;
            } catch (IOException e) {
                logger.error("Failed to load job from disk: {}", jobId, e);
            }
        }
        return null;
    }

    public Collection<ExtractionJob> getAllActive() {
        return activeJobs.values();
    }
}
