package com.tearsdeepmind.service;

import com.tearsdeepmind.dto.yahoo.YahooChartResponse;
import com.tearsdeepmind.entity.market.DailyCandleEntity;
import com.tearsdeepmind.entity.market.IntradayCandleEntity;
import com.tearsdeepmind.repository.market.DailyCandleRepository;
import com.tearsdeepmind.repository.market.IntradayCandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
        logger.info("Syncing daily data for {} with range {}", symbol, range);
        try {
            YahooChartResponse response = restClient.get()
                    .uri("/v8/finance/chart/{symbol}?interval=1d&range={range}", symbol, range)
                    .retrieve()
                    .body(YahooChartResponse.class);

            if (response != null && response.chart().result() != null && !response.chart().result().isEmpty()) {
                processYahooResult(response.chart().result().get(0), symbol, true);
            }
        } catch (Exception e) {
            logger.error("Error syncing daily data for {}", symbol, e);
        }
    }

    public void syncIntradayData(String symbol) {
        logger.info("Syncing 5m intraday data for {}", symbol);
        try {
            YahooChartResponse response = restClient.get()
                    .uri("/v8/finance/chart/{symbol}?interval=5m&range=1d", symbol)
                    .retrieve()
                    .body(YahooChartResponse.class);

            if (response != null && response.chart().result() != null && !response.chart().result().isEmpty()) {
                processYahooResult(response.chart().result().get(0), symbol, false);
            }
        } catch (Exception e) {
            logger.error("Error syncing intraday data for {}", symbol, e);
        }
    }

    private void processYahooResult(YahooChartResponse.Result result, String symbol, boolean isDaily) {
        List<Long> timestamps = result.timestamp();
        YahooChartResponse.Quote quote = result.indicators().quote().get(0);

        List<Double> opens = quote.open();
        List<Double> highs = quote.high();
        List<Double> lows = quote.low();
        List<Double> closes = quote.close();
        List<Long> volumes = quote.volume();

        for (int i = 0; i < timestamps.size(); i++) {
            if (opens.get(i) == null || closes.get(i) == null) continue;

            if (isDaily) {
                LocalDate date = Instant.ofEpochSecond(timestamps.get(i))
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                
                if (dailyCandleRepository.findBySymbolAndDate(symbol, date).isEmpty()) {
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
            } else {
                LocalDateTime dateTime = Instant.ofEpochSecond(timestamps.get(i))
                        .atZone(ZoneId.systemDefault()).toLocalDateTime();
                
                if (intradayCandleRepository.findBySymbolAndTimestamp(symbol, dateTime).isEmpty()) {
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
            }
        }
    }
}
