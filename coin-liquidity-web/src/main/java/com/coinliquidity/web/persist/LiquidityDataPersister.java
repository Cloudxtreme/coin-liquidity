package com.coinliquidity.web.persist;

import com.coinliquidity.web.model.LiquidityData;
import com.coinliquidity.web.model.LiquiditySummary;
import com.coinliquidity.web.model.ViewType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LiquidityDataPersister {

    void persist(final LiquidityData liquidityData);

    Optional<LiquidityData> loadLatest();

    List<LiquiditySummary> loadSummary(final String baseCcy,
                                       final Instant threshold,
                                       final String exchange,
                                       final int bidAskPercent,
                                       final ViewType viewType);
}
