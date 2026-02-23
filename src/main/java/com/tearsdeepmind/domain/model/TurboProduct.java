package com.tearsdeepmind.domain.model;

import java.math.BigDecimal;

public record TurboProduct(
    String isin,
    String direction, // LONG/SHORT
    BigDecimal strike,
    BigDecimal barrier, // Knock-Out Level
    BigDecimal leverage,
    BigDecimal bid,
    BigDecimal ask,
    BigDecimal ratio
) {}
