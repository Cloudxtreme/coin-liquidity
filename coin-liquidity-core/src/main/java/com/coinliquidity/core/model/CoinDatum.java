package com.coinliquidity.core.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CoinDatum {

    private Instant runDate;
    private String symbol;
    private BigDecimal priceUsd;
    private BigDecimal priceBtc;
    private BigDecimal volume24hUsd;
    private BigDecimal marketCapUsd;
    private BigDecimal availableSupply;
    private BigDecimal totalSupply;
    private BigDecimal maxSupply;
    private Instant lastUpdated;

}
