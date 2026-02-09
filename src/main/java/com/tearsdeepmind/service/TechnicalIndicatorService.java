package com.tearsdeepmind.service;

import com.tearsdeepmind.entity.market.DailyCandleEntity;
import com.tearsdeepmind.entity.market.TechnicalIndicatorEntity;
import com.tearsdeepmind.repository.market.DailyCandleRepository;
import com.tearsdeepmind.repository.market.TechnicalIndicatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class TechnicalIndicatorService {
    private static final Logger logger = LoggerFactory.getLogger(TechnicalIndicatorService.class);
    private final DailyCandleRepository dailyCandleRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;

    public TechnicalIndicatorService(DailyCandleRepository dailyCandleRepository,
                                     TechnicalIndicatorRepository technicalIndicatorRepository) {
        this.dailyCandleRepository = dailyCandleRepository;
        this.technicalIndicatorRepository = technicalIndicatorRepository;
    }

    public void calculateAndSaveIndicators(String symbol, LocalDate date) {
        logger.info("Calculating technical indicators for {} on {}", symbol, date);
        
        List<DailyCandleEntity> candles = dailyCandleRepository.findBySymbolOrderByDateDesc(symbol);
        Collections.reverse(candles); // ta4j needs chronological order

        if (candles.size() < 200) {
            logger.warn("Not enough candles for calculation of full indicator set (need 200, have {})", candles.size());
        }

        BarSeries series = new BaseBarSeries(symbol);
        for (DailyCandleEntity c : candles) {
            ZonedDateTime zdt = c.getDate().atStartOfDay(ZoneId.systemDefault());
            series.addBar(zdt, c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume());
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        TechnicalIndicatorEntity entity = technicalIndicatorRepository.findBySymbolAndDate(symbol, date)
                .orElse(new TechnicalIndicatorEntity());
        
        entity.setSymbol(symbol);
        entity.setDate(date);

        // Calculate Daily Indicators
        if (series.getBarCount() >= 9) entity.setEma9d(BigDecimal.valueOf(new EMAIndicator(closePrice, 9).getValue(series.getEndIndex()).doubleValue()));
        if (series.getBarCount() >= 21) entity.setEma21d(BigDecimal.valueOf(new EMAIndicator(closePrice, 21).getValue(series.getEndIndex()).doubleValue()));
        if (series.getBarCount() >= 50) entity.setSma50d(BigDecimal.valueOf(new SMAIndicator(closePrice, 50).getValue(series.getEndIndex()).doubleValue()));
        if (series.getBarCount() >= 200) entity.setSma200d(BigDecimal.valueOf(new SMAIndicator(closePrice, 200).getValue(series.getEndIndex()).doubleValue()));

        // Weekly EMA (Simplified Resampling logic)
        // In production we would use a more robust resampling, here we approximate with SMA 100d or actual weekly bars
        // For this POC, we will use a dedicated series if we have enough data or just approximate
        entity.setEma21w(calculateWeeklyEma(series, 21));

        technicalIndicatorRepository.save(entity);
    }

    private BigDecimal calculateWeeklyEma(BarSeries dailySeries, int period) {
        // Simple approximation for the POC: 21 weeks ~ 105 daily bars
        // A more complex resampling would group bars by week
        if (dailySeries.getBarCount() < 105) return null;
        ClosePriceIndicator close = new ClosePriceIndicator(dailySeries);
        EMAIndicator weeklyApprox = new EMAIndicator(close, 105);
        return BigDecimal.valueOf(weeklyApprox.getValue(dailySeries.getEndIndex()).doubleValue());
    }
}
