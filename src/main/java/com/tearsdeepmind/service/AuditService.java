package com.tearsdeepmind.service;

import com.tearsdeepmind.domain.model.QuantMemoryRecord;
import com.tearsdeepmind.entity.QuantMemoryEntity;
import com.tearsdeepmind.entity.market.AuditLogEntity;
import com.tearsdeepmind.entity.market.DailyCandleEntity;
import com.tearsdeepmind.entity.market.IntradayCandleEntity;
import com.tearsdeepmind.repository.PredictionRepository;
import com.tearsdeepmind.repository.QuantMemoryRepository;
import com.tearsdeepmind.repository.market.AuditLogRepository;
import com.tearsdeepmind.repository.market.DailyCandleRepository;
import com.tearsdeepmind.repository.market.IntradayCandleRepository;
import com.tearsdeepmind.repository.market.TechnicalIndicatorRepository;
import com.tearsdeepmind.repository.ValidationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final PredictionRepository predictionRepository;
    private final QuantMemoryRepository quantMemoryRepository;
    private final DailyCandleRepository dailyCandleRepository;
    private final IntradayCandleRepository intradayCandleRepository;
    private final AuditLogRepository auditLogRepository;
    private final ValidationRepository validationRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final TearsAgentService tearsAgentService;

    public AuditService(PredictionRepository predictionRepository,
                        QuantMemoryRepository quantMemoryRepository,
                        DailyCandleRepository dailyCandleRepository,
                        IntradayCandleRepository intradayCandleRepository,
                        AuditLogRepository auditLogRepository,
                        ValidationRepository validationRepository,
                        TechnicalIndicatorRepository technicalIndicatorRepository,
                        TearsAgentService tearsAgentService) {
        this.predictionRepository = predictionRepository;
        this.quantMemoryRepository = quantMemoryRepository;
        this.dailyCandleRepository = dailyCandleRepository;
        this.intradayCandleRepository = intradayCandleRepository;
        this.auditLogRepository = auditLogRepository;
        this.validationRepository = validationRepository;
        this.technicalIndicatorRepository = technicalIndicatorRepository;
        this.tearsAgentService = tearsAgentService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditDay(LocalDate date) {
        logger.info("Starting audit for {}", date);

        predictionRepository.findByReportDate(date).stream().findFirst().ifPresent(prediction -> {
            dailyCandleRepository.findBySymbolAndDate("^GSPC", date).ifPresent(market -> {
                quantMemoryRepository.findById(date).ifPresent(quant -> {
                    logger.debug("Auditing Date: {}", date);
                    logger.debug("Prediction: {}", prediction);
                    logger.debug("Quant: {}", quant);
                    logger.debug("Market: {}", market);
                    performAudit(date, prediction, quant, market);
                });
            });
        });
    }

    private void performAudit(LocalDate date, com.tearsdeepmind.entity.PredictionEntity prediction, QuantMemoryEntity quant, DailyCandleEntity market) {
        com.tearsdeepmind.entity.market.AuditLogEntity log = auditLogRepository.findByDate(date).orElse(new com.tearsdeepmind.entity.market.AuditLogEntity());
        log.setDate(date);

        // 1. Directional Accuracy with Robust Logic
        String rawDirection = (String) prediction.getPayload().get("direction");
        String predictedDirection = rawDirection != null ? rawDirection.toUpperCase().trim() : "FLAT";
        
        BigDecimal close = market.getClose();
        BigDecimal open = market.getOpen();
        BigDecimal changePct = close.subtract(open).divide(open, 4, RoundingMode.HALF_UP).abs();
        boolean isMarketUp = close.compareTo(open) > 0;
        boolean isFlat = changePct.doubleValue() < 0.0025; // Less than 0.25% move considered FLAT

        boolean isCorrect = false;
        if (predictedDirection.contains("UP") || predictedDirection.contains("BULL")) {
            isCorrect = isMarketUp && !isFlat;
        } else if (predictedDirection.contains("DOWN") || predictedDirection.contains("BEAR")) {
            isCorrect = !isMarketUp && !isFlat;
        } else {
            // FLAT, NEUTRAL, SIDEWAYS
            isCorrect = isFlat;
        }
        
        log.setDirectionCorrect(isCorrect);
        logger.info("Audit Direction for {}: Pred={}, MarketUp={}, Flat={}, Correct={}", date, predictedDirection, isMarketUp, isFlat, isCorrect);

        // 2. Level Precision Score
        double score = calculateLevelPrecision(date, quant.getData().extracted_levels());
        log.setLevelPrecisionScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));

        // 3. Verdict Summary
        StringBuilder verdict = new StringBuilder();
        verdict.append(isCorrect ? "✅ Dirección acertada. " : "❌ Dirección errónea. ");
        verdict.append(String.format("Precisión de niveles: %.1f/10. ", score));
        
        log.setVerdictSummary(verdict.toString());
        AuditLogEntity savedLog = auditLogRepository.save(log);
        logger.info("POST-SAVE CHECK: ID={}, DirCorrect={}", savedLog.getId(), savedLog.getDirectionCorrect());

        // POST-MORTEM (Cognitive Feedback Loop)
        String cognitiveLesson = "None.";
        if (!isCorrect) {
            String actualDir = isFlat ? "FLAT" : (isMarketUp ? "UP" : "DOWN");
            double change = changePct.doubleValue() * (isMarketUp ? 100 : -100);
            
            // Build simple tech context for AI
            String techContext = technicalIndicatorRepository.findBySymbolAndDate("^GSPC", date)
                .map(ti -> String.format("RSI: %.2f | BB_Width: %.4f | EMA9: %.2f", ti.getRsi14d(), ti.getBbWidth(), ti.getEma9d()))
                .orElse("No tech data");

            cognitiveLesson = tearsAgentService.generatePostMortem(predictedDirection, actualDir, change, techContext);
            logger.warn("Cognitive Post-Mortem Lesson: {}", cognitiveLesson);
        }

        // NEW: Persist detailed ValidationEntity
        com.tearsdeepmind.entity.ValidationEntity validation = new com.tearsdeepmind.entity.ValidationEntity(
            prediction.getId(),
            log.getLevelPrecisionScore(),
            null, // errorMargin (can be calculated later if needed)
            log.getVerdictSummary(),
            cognitiveLesson // Save lesson in notes!
        );
        validationRepository.save(validation);

        logger.info("Audit saved for {}: Score {}", date, score);
    }

    private double calculateLevelPrecision(LocalDate date, List<QuantMemoryRecord.ExtractedLevel> levels) {
        System.out.println("Calculating level precision for " + date + ". Levels: " + levels);
        if (levels == null || levels.isEmpty()) {
            System.out.println("No levels provided, returning 0.0");
            return 0.0;
        }
        
        LocalDateTime start = date.atTime(LocalTime.MIN);
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<IntradayCandleEntity> candles = intradayCandleRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc("^GSPC", start, end);
        
        System.out.println("Intraday candles for " + date + ": " + candles.size());
        if (candles.isEmpty()) {
            System.out.println("No intraday candles found, returning 0.0");
            return 0.0;
        }

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
