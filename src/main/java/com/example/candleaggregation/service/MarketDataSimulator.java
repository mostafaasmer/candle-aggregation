package com.example.candleaggregation.service;

import com.example.candleaggregation.model.BidAskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MarketDataSimulator {

    private static final Logger log = LoggerFactory.getLogger(MarketDataSimulator.class);

    private static final String[] SYMBOLS = {"BTC-USD", "ETH-USD", "SOL-USD"};

    // Per-symbol mid-prices for random-walk simulation; accessed only from the scheduler thread
    private final double[] midPrices = {65_000.0, 3_500.0, 180.0};

    private final CandleAggregator aggregator;
    private final Executor executor;

    public MarketDataSimulator(CandleAggregator aggregator,
                               @Qualifier("candleExecutor") Executor executor) {
        this.aggregator = aggregator;
        this.executor = executor;
    }

    // Fires 5 times per second; each symbol event is dispatched to the thread pool
    @Scheduled(fixedRate = 30000)
    public void generateTick() {
        long now = System.currentTimeMillis() / 1000L;
        for (int i = 0; i < SYMBOLS.length; i++) {
            midPrices[i] *= 1.0 + ThreadLocalRandom.current().nextGaussian() * 0.0005;
            double spread = midPrices[i] * 0.0001;
            BidAskEvent event = new BidAskEvent(
                SYMBOLS[i],
                midPrices[i] - spread,
                midPrices[i] + spread,
                now
            );
            log.debug("Simulated tick {} bid={} ask={}", event.symbol(), event.bid(), event.ask());
            executor.execute(() -> aggregator.process(event));
        }
    }
}
