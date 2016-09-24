package com.coinliquidity.web.model;

import com.coinliquidity.web.IllegalFilterException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LiquidityData {

    private List<LiquidityDatum> liquidityData;
    private Date updateTime;
    private BigDecimal amount;
    private Set<String> validBaseCurrencies;
    private Set<String> validQuoteCurrencies;
    private Set<String> validExchanges;

    public List<LiquidityDatum> getLiquidityData() {
        return liquidityData;
    }

    public void setLiquidityData(final List<LiquidityDatum> liquidityData) {
        validBaseCurrencies = liquidityData.stream().map(LiquidityDatum::getBaseCurrency).collect(Collectors.toSet());
        validQuoteCurrencies = liquidityData.stream().map(LiquidityDatum::getQuoteCurrency).collect(Collectors.toSet());
        validExchanges = liquidityData.stream().map(LiquidityDatum::getExchange).collect(Collectors.toSet());
        this.liquidityData = liquidityData;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(final Date updateTime) {
        this.updateTime = updateTime;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public LiquidityData filter(final String baseCurrency, final String quoteCurrency) {
        if (isValid(baseCurrency, validBaseCurrencies) && isValid(quoteCurrency, validQuoteCurrencies)) {
            return filter(datum -> datum.matches(baseCurrency, quoteCurrency));
        } else {
            throw new IllegalFilterException("Invalid base or quote currency");
        }
    }

    public LiquidityData filter(final String exchange) {
        if (isValid(exchange, validExchanges)) {
            return filter(datum -> datum.matches(exchange));
        } else {
            throw new IllegalFilterException("Invalid exchange");
        }
    }

    private boolean isValid(final String filter, final Set<String> values) {
        return "*".equals(filter) || values.contains(filter);
    }

    private LiquidityData filter(final Predicate<LiquidityDatum> predicate) {
        final LiquidityData liquidityData = new LiquidityData();
        liquidityData.setUpdateTime(this.updateTime);
        liquidityData.setAmount(this.amount);
        final List<LiquidityDatum> filteredDatums = this.liquidityData.stream()
                .filter(predicate).collect(Collectors.toList());
        liquidityData.setLiquidityData(filteredDatums);
        return liquidityData;
    }
}
