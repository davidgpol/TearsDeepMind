package com.tearsdeepmind.service;

import com.tearsdeepmind.domain.model.MarketMemoryRecord;
import com.tearsdeepmind.domain.model.QuantMemoryRecord;
import com.tearsdeepmind.entity.DailyAnalysisEntity;
import com.tearsdeepmind.entity.QuantMemoryEntity;
import com.tearsdeepmind.repository.DailyAnalysisRepository;
import com.tearsdeepmind.repository.QuantMemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class HistoryService {

    private final DailyAnalysisRepository dailyAnalysisRepository;
    private final QuantMemoryRepository quantMemoryRepository;

    public HistoryService(DailyAnalysisRepository dailyAnalysisRepository, QuantMemoryRepository quantMemoryRepository) {
        this.dailyAnalysisRepository = dailyAnalysisRepository;
        this.quantMemoryRepository = quantMemoryRepository;
    }

    // Daily Analysis
    public List<DailyAnalysisEntity> getAllDailyAnalysis() {
        return dailyAnalysisRepository.findAll();
    }

    public Optional<DailyAnalysisEntity> getDailyAnalysis(LocalDate date) {
        return dailyAnalysisRepository.findById(date);
    }

    @Transactional
    public DailyAnalysisEntity saveDailyAnalysis(LocalDate date, MarketMemoryRecord data) {
        DailyAnalysisEntity entity = new DailyAnalysisEntity(date, data, null, "v1", "manual");
        return dailyAnalysisRepository.save(entity);
    }

    @Transactional
    public void deleteDailyAnalysis(LocalDate date) {
        dailyAnalysisRepository.deleteById(date);
    }

    // Quant Memory
    public List<QuantMemoryEntity> getAllQuantMemory() {
        return quantMemoryRepository.findAll();
    }

    public Optional<QuantMemoryEntity> getQuantMemory(LocalDate date) {
        return quantMemoryRepository.findById(date);
    }

    @Transactional
    public QuantMemoryEntity saveQuantMemory(LocalDate date, QuantMemoryRecord data) {
        QuantMemoryEntity entity = new QuantMemoryEntity(date, data, null, "v1", "manual");
        return quantMemoryRepository.save(entity);
    }

    @Transactional
    public void deleteQuantMemory(LocalDate date) {
        quantMemoryRepository.deleteById(date);
    }
}