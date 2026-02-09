package com.tearsdeepmind.service;

import com.tearsdeepmind.domain.model.QuantMemoryRecord;
import com.tearsdeepmind.entity.QuantMemoryEntity;
import com.tearsdeepmind.entity.market.AuditLogEntity;
import com.tearsdeepmind.entity.market.DailyCandleEntity;
import com.tearsdeepmind.entity.market.IntradayCandleEntity;
import com.tearsdeepmind.repository.QuantMemoryRepository;
import com.tearsdeepmind.repository.market.AuditLogRepository;
import com.tearsdeepmind.repository.market.DailyCandleRepository;
import com.tearsdeepmind.repository.market.IntradayCandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final QuantMemoryRepository quantMemoryRepository;
    private final DailyCandleRepository dailyCandleRepository;
    private final IntradayCandleRepository intradayCandleRepository;
    private final AuditLogRepository auditLogRepository;

    public AuditService(QuantMemoryRepository quantMemoryRepository,
                        DailyCandleRepository dailyCandleRepository,
                        IntradayCandleRepository intradayCandleRepository,
                        AuditLogRepository auditLogRepository) {
        this.quantMemoryRepository = quantMemoryRepository;
        this.dailyCandleRepository = dailyCandleRepository;
        this.intradayCandleRepository = intradayCandleRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public void auditDay(LocalDate date) {
        logger.info("Starting audit for {}", date);
        
        quantMemoryRepository.findById(date).ifPresent(quant -> {
            dailyCandleRepository.findBySymbolAndDate("^GSPC", date).ifPresent(market -> {
                performAudit(date, quant, market);
            });
        });
    }

    private void performAudit(LocalDate date, QuantMemoryEntity quant, DailyCandleEntity market) {
        AuditLogEntity log = auditLogRepository.findByDate(date).orElse(new AuditLogEntity());
        log.setDate(date);
        
        // 1. Directional Accuracy
        String bias = quant.getData().quant_commentary() != null ? quant.getData().quant_commentary().toUpperCase() : "";
        boolean isUp = market.getClose().compareTo(market.getOpen()) > 0;
        
        if (bias.contains("BULLISH")) {
            log.setDirectionCorrect(isUp);
        } else if (bias.contains("BEARISH")) {
            log.setDirectionCorrect(!isUp);
        }

        // 2. Level Precision Score (Simplified for POC)
        double score = calculateLevelPrecision(date, quant.getData().extracted_levels());
        log.setLevelPrecisionScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));

        // 3. Verdict Summary
        StringBuilder verdict = new StringBuilder();
        verdict.append(log.getDirectionCorrect() != null && log.getDirectionCorrect() ? "✅ Dirección acertada. " : "❌ Dirección errónea. ");
        verdict.append(String.format("Precisión de niveles: %.1f/10. ", score));
        
        log.setVerdictSummary(verdict.toString());
        auditLogRepository.save(log);
        logger.info("Audit saved for {}: Score {}", date, score);
    }

    private double calculateLevelPrecision(LocalDate date, List<QuantMemoryRecord.ExtractedLevel> levels) {
        if (levels == null || levels.isEmpty()) return 0.0;
        
        LocalDateTime start = date.atTime(LocalTime.MIN);
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<IntradayCandleEntity> candles = intradayCandleRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc("^GSPC", start, end);
        
        if (candles.isEmpty()) return 0.0;

        int hits = 0;
        int totalLevelsWithValues = 0;

        for (QuantMemoryRecord.ExtractedLevel level : levels) {
            if (level.numeric_values() == null || level.numeric_values().isEmpty()) continue;
            
            totalLevelsWithValues++;
            double target = level.numeric_values().get(0);
            double tolerance = target * 0.0015; // 0.15% tolerance

            for (IntradayCandleEntity candle : candles) {
                double high = candle.getHigh().doubleValue();
                double low = candle.getLow().doubleValue();
                
                if (target >= (low - tolerance) && target <= (high + tolerance)) {
                    hits++;
                    break; // Level touched at least once
                }
            }
        }

        if (totalLevelsWithValues == 0) return 0.0;
        return (double) hits / totalLevelsWithValues * 10.0;
    }
}
