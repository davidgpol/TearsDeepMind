package com.tearsdeepmind.service;

import com.tearsdeepmind.entity.DailyAnalysisEntity;
import com.tearsdeepmind.entity.QuantMemoryEntity;
import com.tearsdeepmind.repository.DailyAnalysisRepository;
import com.tearsdeepmind.repository.QuantMemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HistoryService {

    private final DailyAnalysisRepository dailyAnalysisRepository;
    private final QuantMemoryRepository quantMemoryRepository;

    public HistoryService(DailyAnalysisRepository dailyAnalysisRepository, QuantMemoryRepository quantMemoryRepository) {
        this.dailyAnalysisRepository = dailyAnalysisRepository;
        this.quantMemoryRepository = quantMemoryRepository;
    }

    // Daily Analysis Operations
    public List<DailyAnalysisEntity> getAllDailyAnalysis() {
        return dailyAnalysisRepository.findAll();
    }

    public Optional<DailyAnalysisEntity> getDailyAnalysis(String date) {
        return dailyAnalysisRepository.findById(date);
    }

    @Transactional
    public DailyAnalysisEntity saveDailyAnalysis(String date, Map<String, Object> data) {
        return dailyAnalysisRepository.save(new DailyAnalysisEntity(date, data));
    }

    @Transactional
    public void deleteDailyAnalysis(String date) {
        dailyAnalysisRepository.deleteById(date);
    }

    // Quant Memory Operations
    public List<QuantMemoryEntity> getAllQuantMemory() {
        return quantMemoryRepository.findAll();
    }

    public Optional<QuantMemoryEntity> getQuantMemory(String date) {
        return quantMemoryRepository.findById(date);
    }

    @Transactional
    public QuantMemoryEntity saveQuantMemory(String date, Map<String, Object> data) {
        return quantMemoryRepository.save(new QuantMemoryEntity(date, data));
    }

    @Transactional
    public void deleteQuantMemory(String date) {
        quantMemoryRepository.deleteById(date);
    }
}
