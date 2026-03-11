package com.tearsdeepmind.entity.market;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_candles", schema = "market_data")
public class DailyCandleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal open;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal high;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal low;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal close;

    private Long volume;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public DailyCandleEntity() {}

    public DailyCandleEntity(String symbol, LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume) {
        this.symbol = symbol;
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public LocalDate getDate() { return date; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
}
