package com.tearsdeepmind.entity.market;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "technical_indicators", schema = "market_data")
public class TechnicalIndicatorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "ema_9d", precision = 10, scale = 2)
    private BigDecimal ema9d;

    @Column(name = "ema_21d", precision = 10, scale = 2)
    private BigDecimal ema21d;

    @Column(name = "sma_50d", precision = 10, scale = 2)
    private BigDecimal sma50d;

    @Column(name = "sma_200d", precision = 10, scale = 2)
    private BigDecimal sma200d;

    @Column(name = "ema_21w", precision = 10, scale = 2)
    private BigDecimal ema21w;

    @Column(name = "atr_14d", precision = 10, scale = 2)
    private BigDecimal atr14d;

    @Column(name = "rsi_14d", precision = 10, scale = 2)
    private BigDecimal rsi14d;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public TechnicalIndicatorEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getEma9d() { return ema9d; }
    public void setEma9d(BigDecimal ema9d) { this.ema9d = ema9d; }
    public BigDecimal getEma21d() { return ema21d; }
    public void setEma21d(BigDecimal ema21d) { this.ema21d = ema21d; }
    public BigDecimal getSma50d() { return sma50d; }
    public void setSma50d(BigDecimal sma50d) { this.sma50d = sma50d; }
    public BigDecimal getSma200d() { return sma200d; }
    public void setSma200d(BigDecimal sma200d) { this.sma200d = sma200d; }
    public BigDecimal getEma21w() { return ema21w; }
    public void setEma21w(BigDecimal ema21w) { this.ema21w = ema21w; }
    public BigDecimal getAtr14d() { return atr14d; }
    public void setAtr14d(BigDecimal atr14d) { this.atr14d = atr14d; }
    public BigDecimal getRsi14d() { return rsi14d; }
    public void setRsi14d(BigDecimal rsi14d) { this.rsi14d = rsi14d; }
}
