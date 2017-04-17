package com.coinliquidity.web.persist;

import com.coinliquidity.web.model.LiquidityData;
import com.coinliquidity.web.model.LiquidityDatum;
import com.coinliquidity.web.model.LiquiditySummary;
import com.coinliquidity.web.model.ViewType;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.coinliquidity.core.analyzer.BidAskAnalyzer.PERCENTAGES;
import static com.coinliquidity.core.util.DecimalUtils.avgPrice;
import static com.coinliquidity.core.util.DecimalUtils.percentDiff;
import static com.coinliquidity.core.util.ResourceUtils.resource;

public class DbPersister implements LiquidityDataPersister {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbPersister.class);

    private static final LiquidityDatumRowMapper LIQUIDITY_DATUM_MAPPER = new LiquidityDatumRowMapper();
    private static final LiquiditySummaryRowMapper LIQUIDITY_SUMMARY_MAPPER = new LiquiditySummaryRowMapper();

    private static final String INSERT = resource("database/insert.sql");
    private static final String SELECT_SINCE = resource("database/select_since.sql");
    private static final String SELECT_LATEST = resource("database/select_latest.sql");
    private static final String SELECT_LIQUIDITY_SUMMARY = resource("database/select_liquidity_summary.sql");
    private static final String SELECT_PRICE_SUMMARY = resource("database/select_price_summary.sql");

    private final JdbcTemplate jdbcTemplate;

    public DbPersister(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.execute("database/create.sql");
        this.execute("database/create_index.sql");
        this.execute("database/execute.sql");
    }

    @Override
    public void persist(final LiquidityData liquidityData) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final List<Object[]> batchArgs = Lists.newArrayList();
        for (final LiquidityDatum datum : liquidityData.getLiquidityData()) {
            batchArgs.add(toArgs(liquidityData, datum));
        }
        jdbcTemplate.batchUpdate(INSERT, batchArgs);
        LOGGER.info("persist took {}", stopwatch.stop());
    }

    private Object[] toArgs(final LiquidityData liquidityData, final LiquidityDatum datum) {
        final List<Object> args = new ArrayList<>();
        args.add(Timestamp.from(liquidityData.getUpdateTime()));
        args.add(datum.getExchange());
        args.add(datum.getCurrencyPair().getBaseCurrency());
        args.add(datum.getCurrencyPair().getQuoteCurrency());
        args.add(datum.getSellCost());
        args.add(datum.getBuyCost());
        args.add(datum.getBestBid());
        args.add(datum.getBestAsk());
        args.add(datum.getPrice());

        PERCENTAGES.forEach(percent -> {
            args.add(datum.getBids(percent));
            args.add(datum.getBidsUsd(percent));
            args.add(datum.getAsks(percent));
            args.add(datum.getAsksUsd(percent));
        });

        return args.toArray();
    }

    @Override
    public Optional<LiquidityData> loadLatest() {
        final List<LiquidityDatum> datums = jdbcTemplate.query(SELECT_LATEST, LIQUIDITY_DATUM_MAPPER);
        if (!datums.isEmpty()) {
            final LiquidityData liquidityData = new LiquidityData();
            liquidityData.setUpdateTime(datums.get(0).getUpdateTime());
            liquidityData.setLiquidityData(datums);
            return Optional.of(liquidityData);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public List<LiquidityData> loadHistory(final Instant threshold) {
        final List<LiquidityDatum> datums = jdbcTemplate.query(SELECT_SINCE,
                new Object[] { Timestamp.from(threshold) }, LIQUIDITY_DATUM_MAPPER);

        final Map<Instant, List<LiquidityDatum>> grouped =
                new TreeMap<>(datums.stream().collect(Collectors.groupingBy(LiquidityDatum::getUpdateTime)));

        return grouped.entrySet().stream().map(entry -> {
            final LiquidityData liquidityData = new LiquidityData();
            liquidityData.setUpdateTime(entry.getKey());
            liquidityData.setLiquidityData(entry.getValue());
            return liquidityData;
        }).collect(Collectors.toList());
    }

    @Override
    public List<LiquiditySummary> loadSummary(final String baseCcy,
                                              final Instant threshold,
                                              final String exchange,
                                              final int bidAskPercent,
                                              final ViewType viewType) {
        final Stopwatch stopwatch = Stopwatch.createStarted();

        final boolean exchangeFilter = exchange != null;
        final Object[] args = new Object[] { baseCcy, Timestamp.from(threshold), exchangeFilter, exchange };

        String bidsColumn;
        String asksColumn;


        if (bidAskPercent == 0) {
            bidsColumn = "total_bids";
            asksColumn = "total_asks";
        } else {
            bidsColumn = "bids_" + bidAskPercent;
            asksColumn = "asks_" + bidAskPercent;
        }

        if (ViewType.USD.equals(viewType)) {
            bidsColumn += "_usd";
            asksColumn += "_usd";
        }  else if (ViewType.DEFAULT.equals(viewType)) {
            bidsColumn += "_usd";
        }

        final String sql = SELECT_LIQUIDITY_SUMMARY
                .replaceAll("<bids_column>", bidsColumn)
                .replaceAll("<asks_column>", asksColumn);

        final List<LiquiditySummary> liquiditySummaries = jdbcTemplate.query(sql, args, LIQUIDITY_SUMMARY_MAPPER);

        final Map<Instant, LiquiditySummary> map = liquiditySummaries.stream()
                .collect(Collectors.toMap(LiquiditySummary::getUpdateTime, Function.identity()));

        jdbcTemplate.query(SELECT_PRICE_SUMMARY, args, rs -> {
            final Instant runDate = rs.getTimestamp("run_date").toInstant();
            final BigDecimal avgBid = rs.getBigDecimal("avg_bid");
            final BigDecimal avgAsk = rs.getBigDecimal("avg_ask");

            final LiquiditySummary liquiditySummary = map.get(runDate);
            if (liquiditySummary != null) {
                if (BigDecimal.TEN.compareTo(percentDiff(avgBid, avgAsk)) > 0) {
                    liquiditySummary.setPrice(avgPrice(avgBid, avgAsk));
                }
            }
        });

        LOGGER.info("loadSummary took {}", stopwatch.stop());
        return liquiditySummaries;
    }

    private void execute(final String scriptName) {
        final String sql = resource(scriptName);

        if (StringUtils.isNotBlank(sql)) {
            final Stopwatch stopwatch = Stopwatch.createStarted();
            LOGGER.info("Executing {}", scriptName);

            final String[] lines = sql.split(";\\n");

            LOGGER.info("{} lines in {}", lines.length, scriptName);

            try {
                Arrays.stream(lines).forEach(jdbcTemplate::execute);
            } catch (final Exception e) {
                LOGGER.error("Could not execute {}", scriptName, e);
            }

            LOGGER.info("Finished {} in {}", scriptName, stopwatch.stop());
        } else {
            LOGGER.info("Nothing to execute in {}", scriptName);
        }
    }
}
