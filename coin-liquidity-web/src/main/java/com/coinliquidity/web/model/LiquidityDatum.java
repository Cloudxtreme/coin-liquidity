package com.coinliquidity.web.model;

import com.coinliquidity.core.model.CurrencyPair;
import com.coinliquidity.core.util.DecimalUtils;
import com.google.common.collect.Maps;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

import static com.coinliquidity.core.analyzer.BidAskAnalyzer.PERCENTAGES;
import static com.coinliquidity.core.model.CurrencyPair.BTC;

@Data
public class LiquidityDatum {

    private String exchange;
    private CurrencyPair currencyPair;
    private BigDecimal bestAsk;
    private BigDecimal bestBid;
    private Instant updateTime;
    private BigDecimal price;
    private BigDecimal liquidityScore;

    private final Map<Integer, BigDecimal> bids = Maps.newHashMap();
    private final Map<Integer, BigDecimal> asks = Maps.newHashMap();
    private final Map<Integer, BigDecimal> bidsUsd = Maps.newHashMap();
    private final Map<Integer, BigDecimal> asksUsd = Maps.newHashMap();

    public BigDecimal getBids(final int percent) {
        return bids.get(percent);
    }

    public BigDecimal getAsks(final int percent) {
        return asks.get(percent);
    }

    public void setBids(final int percent, final BigDecimal value) {
        this.bids.put(percent, value);
    }

    public void setAsks(final int percent, final BigDecimal value) {
        this.asks.put(percent, value);
    }

    public BigDecimal getBidsUsd(final int percent) {
        return bidsUsd.get(percent);
    }

    public BigDecimal getAsksUsd(final int percent) {
        return asksUsd.get(percent);
    }

    public void setBidsUsd(final int percent, final BigDecimal value) {
        this.bidsUsd.put(percent, value);
    }

    public void setAsksUsd(final int percent, final BigDecimal value) {
        this.asksUsd.put(percent, value);
    }

    public BigDecimal getTotalAsks() {
        return getAsks(0);
    }

    public BigDecimal getTotalBids() {
        return getBids(0);
    }

    public BigDecimal getTotalAsksUsd() {
        return getAsksUsd(0);
    }
    public BigDecimal getTotalBidsUsd() {
        return getBidsUsd(0);
    }

    public BigDecimal getLiquidityScore() {
        if (this.liquidityScore == null) {
            this.liquidityScore = score(bidsUsd).add(score(asksUsd))
                    .divide(DecimalUtils.TWO, 0, RoundingMode.HALF_UP);
        }
        return this.liquidityScore;
    }

    private BigDecimal score(final Map<Integer, BigDecimal> values) {
        BigDecimal score = BigDecimal.ZERO;
        BigDecimal prev = BigDecimal.ZERO;
        for (final Integer percent : PERCENTAGES) {
            if (percent != 0 && values.containsKey(percent)) {
                final BigDecimal current = values.get(percent);
                final BigDecimal diff = current.subtract(prev);
                score = score.add(diff.divide(BigDecimal.valueOf(percent), 0, RoundingMode.HALF_UP));
                prev = current;
            }
        }
        return score;
    }

    public boolean matches(final String baseCurrency, final String quoteCurrency) {
        return isMatch(baseCurrency, currencyPair.getBaseCurrency())
                && isMatch(quoteCurrency, currencyPair.getQuoteCurrency());
    }

    public boolean matches(final String exchange) {
        return "*".equals(exchange) || this.exchange.equals(exchange);
    }

    private static boolean isMatch(final String testCurrency, final String currency) {
        return "*".equals(testCurrency) || currency.equals(testCurrency) ||
                ("ALT".equals(testCurrency) && !BTC.equals(currency));
    }

    public String getBaseCurrency() {
        return currencyPair.getBaseCurrency();
    }

    String getQuoteCurrency() {
        return currencyPair.getQuoteCurrency();
    }
}
