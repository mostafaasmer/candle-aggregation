package com.example.candleaggregation.service;

import com.example.candleaggregation.model.BidAskEvent;
import com.example.candleaggregation.model.Candle;
import com.example.candleaggregation.model.Interval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CandleAggregatorTest {

    private CandleStorageService storageService;
    private CandleAggregator aggregator;

    @BeforeEach
    void setUp() {
        storageService = new CandleStorageService();
        aggregator = new CandleAggregator(storageService);
    }

    @Test
    void noLiveCandleBeforeAnyEvent() {
        assertNull(aggregator.getLiveCandle("BTC-USD", Interval.ONE_MINUTE));
    }

    @Test
    void singleTickOpensCandle() {
        aggregator.process(new BidAskEvent("BTC-USD", 100.0, 102.0, 1_000_000L));

        Candle live = aggregator.getLiveCandle("BTC-USD", Interval.ONE_SECOND);
        assertNotNull(live);
        assertEquals(101.0, live.open(), 0.001);   // mid = (100+102)/2
        assertEquals(101.0, live.high(), 0.001);
        assertEquals(101.0, live.low(), 0.001);
        assertEquals(101.0, live.close(), 0.001);
        assertEquals(1L, live.volume());
    }

    @Test
    void midPriceIsAverageOfBidAndAsk() {
        aggregator.process(new BidAskEvent("ETH-USD", 200.0, 300.0, 2_000_000L));

        Candle live = aggregator.getLiveCandle("ETH-USD", Interval.ONE_SECOND);
        assertNotNull(live);
        assertEquals(250.0, live.open(), 0.001);
    }

    @Test
    void multipleTicksInSameWindowUpdateOHLC() {
        aggregator.process(new BidAskEvent("BTC-USD", 100.0, 102.0, 1_000_000L)); // mid=101
        aggregator.process(new BidAskEvent("BTC-USD", 108.0, 110.0, 1_000_000L)); // mid=109 (new high)
        aggregator.process(new BidAskEvent("BTC-USD",  94.0,  96.0, 1_000_000L)); // mid=95  (new low)

        Candle live = aggregator.getLiveCandle("BTC-USD", Interval.ONE_SECOND);
        assertNotNull(live);
        assertEquals(101.0, live.open(),  0.001);  // first tick
        assertEquals(109.0, live.high(),  0.001);
        assertEquals(95.0,  live.low(),   0.001);
        assertEquals(95.0,  live.close(), 0.001);  // last tick
        assertEquals(3L, live.volume());
    }

    @Test
    void newWindowFinalizesOldCandle() {
        aggregator.process(new BidAskEvent("BTC-USD", 100.0, 102.0, 1_000_000L)); // window=1000000s
        aggregator.process(new BidAskEvent("BTC-USD", 108.0, 110.0, 1_000_001L)); // triggers finalization

        List<Candle> stored = storageService.getCandles(
            "BTC-USD", Interval.ONE_SECOND, 1_000_000L, 1_000_000L);
        assertEquals(1, stored.size());
        Candle finalized = stored.get(0);
        assertEquals(1_000_000L, finalized.time());
        assertEquals(101.0, finalized.open(), 0.001);
        assertEquals(1L, finalized.volume());
    }

    @Test
    void oneEventOpensAllIntervals() {
        aggregator.process(new BidAskEvent("BTC-USD", 100.0, 102.0, 1_000_000L));

        for (Interval interval : Interval.values()) {
            assertNotNull(aggregator.getLiveCandle("BTC-USD", interval),
                "Expected live candle for " + interval.getLabel());
        }
    }

    @Test
    void differentSymbolsTrackedIndependently() {
        aggregator.process(new BidAskEvent("BTC-USD", 100.0, 102.0, 1_000_000L));

        assertNotNull(aggregator.getLiveCandle("BTC-USD", Interval.ONE_SECOND));
        assertNull(aggregator.getLiveCandle("ETH-USD", Interval.ONE_SECOND));
    }

    @Test
    void windowAlignmentFor5sInterval() {
        // t=1000003 → windowStart = (1000003/5)*5 = 1000000
        aggregator.process(new BidAskEvent("BTC-USD", 100.0, 102.0, 1_000_003L));

        Candle live = aggregator.getLiveCandle("BTC-USD", Interval.FIVE_SECONDS);
        assertNotNull(live);
        assertEquals(1_000_000L, live.time());
    }
}
