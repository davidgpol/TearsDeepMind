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
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        if (candles.size() < 20) {
            logger.warn("Not enough candles for calculation of indicators for {} on {} (need 20, have {})", symbol, date, candles.size());
            return;
        }

        BarSeries series = new BaseBarSeries(symbol);
        int targetIndex = -1;
        
        for (int i = 0; i < candles.size(); i++) {
            DailyCandleEntity c = candles.get(i);
            ZonedDateTime zdt = c.getDate().atStartOfDay(ZoneId.systemDefault());
            series.addBar(zdt, c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume());
            
            if (c.getDate().equals(date)) {
                targetIndex = i;
            }
        }

        if (targetIndex == -1) {
            logger.warn("Target date {} not found in candle series for {}", date, symbol);
            return;
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        TechnicalIndicatorEntity entity = technicalIndicatorRepository.findBySymbolAndDate(symbol, date)
                .orElse(new TechnicalIndicatorEntity());
        
        entity.setSymbol(symbol);
        entity.setDate(date);

        // Calculate Daily Indicators at TARGET INDEX
        if (targetIndex >= 9) entity.setEma9d(BigDecimal.valueOf(new EMAIndicator(closePrice, 9).getValue(targetIndex).doubleValue()));
        if (targetIndex >= 21) entity.setEma21d(BigDecimal.valueOf(new EMAIndicator(closePrice, 21).getValue(targetIndex).doubleValue()));
        if (targetIndex >= 50) entity.setSma50d(BigDecimal.valueOf(new SMAIndicator(closePrice, 50).getValue(targetIndex).doubleValue()));
        if (targetIndex >= 200) entity.setSma200d(BigDecimal.valueOf(new SMAIndicator(closePrice, 200).getValue(targetIndex).doubleValue()));

        // ATR (14)
        if (targetIndex >= 14) {
            ATRIndicator atr = new ATRIndicator(series, 14);
            entity.setAtr14d(BigDecimal.valueOf(atr.getValue(targetIndex).doubleValue()));
        }

        // RSI (14)
        if (targetIndex >= 14) {
            RSIIndicator rsi = new RSIIndicator(closePrice, 14);
            entity.setRsi14d(BigDecimal.valueOf(rsi.getValue(targetIndex).doubleValue()));
        }

        // Bollinger Bands (20, 2)
        if (targetIndex >= 20) {
            SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
            BollingerBandsMiddleIndicator bbm = new BollingerBandsMiddleIndicator(sma20);
            org.ta4j.core.indicators.statistics.StandardDeviationIndicator sd20 = new org.ta4j.core.indicators.statistics.StandardDeviationIndicator(closePrice, 20);
            
            BollingerBandsUpperIndicator bbu = new BollingerBandsUpperIndicator(bbm, sd20, series.numOf(2));
            BollingerBandsLowerIndicator bbl = new BollingerBandsLowerIndicator(bbm, sd20, series.numOf(2));
            
            double upper = bbu.getValue(targetIndex).doubleValue();
            double lower = bbl.getValue(targetIndex).doubleValue();
            double middle = bbm.getValue(targetIndex).doubleValue();
            
            entity.setBbUpper(BigDecimal.valueOf(upper));
            entity.setBbLower(BigDecimal.valueOf(lower));
            
            if (middle != 0) {
                double width = (upper - lower) / middle;
                entity.setBbWidth(BigDecimal.valueOf(width));
            }
        }

        // Weekly EMA Approximation
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
