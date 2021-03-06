SELECT
    run_date,
    AVG(best_bid) AS avg_bid,
    AVG(best_ask) AS avg_ask
FROM liquidity_history
WHERE base_ccy = ? AND run_date > ?
AND (
(0 = ? AND exchange IN ('gdax.com', 'gemini.com', 'poloniex.com', 'bitstamp.net'))
OR (exchange = ?)
)
GROUP BY run_date