package com.tearsdeepmind.service;

import com.tearsdeepmind.dto.yahoo.YahooChartResponse;
import com.tearsdeepmind.entity.market.DailyCandleEntity;
import com.tearsdeepmind.entity.market.IntradayCandleEntity;
import com.tearsdeepmind.repository.market.DailyCandleRepository;
import com.tearsdeepmind.repository.market.IntradayCandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class MarketDataService {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    private final RestClient restClient;
    private final DailyCandleRepository dailyCandleRepository;
    private final IntradayCandleRepository intradayCandleRepository;

    public MarketDataService(RestClient yahooFinanceClient, 
                             DailyCandleRepository dailyCandleRepository,
                             IntradayCandleRepository intradayCandleRepository) {
        this.restClient = yahooFinanceClient;
        this.dailyCandleRepository = dailyCandleRepository;
        this.intradayCandleRepository = intradayCandleRepository;
    }

    public void syncDailyData(String symbol, String range) {
        syncHistoryWithRetry(symbol, range);
    }

    public void syncHistoryWithRetry(String symbol, String range) {
        int maxRetries = 3;
        int attempt = 0;
        long backoff = 2000; // 2 seconds

        while (attempt < maxRetries) {
            try {
                logger.info("Attempt {}/{} to sync history for {} with range {}", attempt + 1, maxRetries, symbol, range);
                YahooChartResponse response = restClient.get()
                        .uri("/v8/finance/chart/{symbol}?interval=1d&range={range}", symbol, range)
                        .retrieve()
                        .body(YahooChartResponse.class);

                if (response != null && response.chart().result() != null && !response.chart().result().isEmpty()) {
                    processYahooResult(response.chart().result().get(0), symbol, true, 0L, 0L);
                    logger.info("Successfully synced history for {}", symbol);
                    return; // Success
                } else {
                    logger.warn("Empty response for {}, retrying...", symbol);
                }
            } catch (Exception e) {
                logger.error("Error syncing history for {} (Attempt {}): {}", symbol, attempt + 1, e.getMessage());
            }

            attempt++;
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(backoff);
                    backoff *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        logger.error("Failed to sync history for {} after {} attempts", symbol, maxRetries);
    }

    public void syncIntradayData(String symbol, LocalDate date) {
        logger.info("Syncing 5m intraday data for {} on {}", symbol, date);
        try {
            long startOfDay = date.atStartOfDay(ZoneId.of("America/New_York")).toEpochSecond();
            long endOfDay = date.plusDays(1).atStartOfDay(ZoneId.of("America/New_York")).toEpochSecond() - 1;

            YahooChartResponse response = restClient.get()
                    .uri("/v8/finance/chart/{symbol}?interval=5m&period1={period1}&period2={period2}", symbol, startOfDay, endOfDay)
                    .retrieve()
                    .body(YahooChartResponse.class);

            if (response != null && response.chart().result() != null && !response.chart().result().isEmpty()) {
                processYahooResult(response.chart().result().get(0), symbol, false, startOfDay, endOfDay);
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.warn("HTTP Error syncing intraday data for {} on {}: Status {} - {}", symbol, date, e.getStatusCode(), e.getStatusText());
        } catch (Exception e) {
            logger.error("Unexpected error syncing intraday data for {} on {}", symbol, date, e);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    protected void processYahooResult(YahooChartResponse.Result result, String symbol, boolean isDaily, long startTimestamp, long endTimestamp) {
        List<Long> timestamps = result.timestamp();
        if (timestamps == null) {
            logger.warn("No timestamps found in Yahoo Finance response for symbol {}", symbol);
            return;
        }

        YahooChartResponse.Quote quote = result.indicators().quote().get(0);

        List<Double> opens = quote.open();
        List<Double> highs = quote.high();
        List<Double> lows = quote.low();
        List<Double> closes = quote.close();
        List<Long> volumes = quote.volume();

        int processedCount = 0;
        for (int i = 0; i < timestamps.size(); i++) {
            Long currentTimestamp = timestamps.get(i);
            if (currentTimestamp == null || opens.get(i) == null || closes.get(i) == null) {
                continue;
            }

            // Apply timestamp filter ONLY for intraday data
            if (!isDaily) {
                if (currentTimestamp < startTimestamp || currentTimestamp > endTimestamp) {
                    continue; // Skip this record as it's outside the requested day
                }
            }

            if (isDaily) {
                LocalDate date = Instant.ofEpochSecond(currentTimestamp)
                        .atZone(ZoneId.of("America/New_York")).toLocalDate();
                
                java.util.Optional<DailyCandleEntity> existingOpt = dailyCandleRepository.findBySymbolAndDate(symbol, date);
                if (existingOpt.isPresent()) {
                    DailyCandleEntity existing = existingOpt.get();
                    existing.setOpen(BigDecimal.valueOf(opens.get(i)));
                    existing.setHigh(BigDecimal.valueOf(highs.get(i)));
                    existing.setLow(BigDecimal.valueOf(lows.get(i)));
                    existing.setClose(BigDecimal.valueOf(closes.get(i)));
                    existing.setVolume(volumes.get(i));
                    dailyCandleRepository.save(existing);
                } else {
                    DailyCandleEntity entity = new DailyCandleEntity(
                        symbol, date,
                        BigDecimal.valueOf(opens.get(i)),
                        BigDecimal.valueOf(highs.get(i)),
                        BigDecimal.valueOf(lows.get(i)),
                        BigDecimal.valueOf(closes.get(i)),
                        volumes.get(i)
                    );
                    dailyCandleRepository.save(entity);
                }
                processedCount++;
            } else {
                LocalDateTime dateTime = Instant.ofEpochSecond(currentTimestamp)
                        .atZone(ZoneId.of("America/New_York")).toLocalDateTime();
                
                java.util.Optional<IntradayCandleEntity> existingOpt = intradayCandleRepository.findBySymbolAndTimestamp(symbol, dateTime);
                if (existingOpt.isPresent()) {
                    IntradayCandleEntity existing = existingOpt.get();
                    existing.setOpen(BigDecimal.valueOf(opens.get(i)));
                    existing.setHigh(BigDecimal.valueOf(highs.get(i)));
                    existing.setLow(BigDecimal.valueOf(lows.get(i)));
                    existing.setClose(BigDecimal.valueOf(closes.get(i)));
                    existing.setVolume(volumes.get(i));
                    intradayCandleRepository.save(existing);
                } else {
                    IntradayCandleEntity entity = new IntradayCandleEntity(
                        symbol, dateTime,
                        BigDecimal.valueOf(opens.get(i)),
                        BigDecimal.valueOf(highs.get(i)),
                        BigDecimal.valueOf(lows.get(i)),
                        BigDecimal.valueOf(closes.get(i)),
                        volumes.get(i)
                    );
                    intradayCandleRepository.save(entity);
                }
                processedCount++;
            }
        }
        logger.info("Processed {} market data records for {}", processedCount, symbol);
    }
}
