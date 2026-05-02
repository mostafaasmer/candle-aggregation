package com.example.candleaggregation.service;

import com.example.candleaggregation.model.Candle;
import com.example.candleaggregation.model.Interval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CandleStorageServiceTest {

    private CandleStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new CandleStorageService();
    }

    @Test
    void storeAndRetrieveSingleCandle() {
        Candle candle = new Candle(1_000L, 100.0, 110.0, 90.0, 105.0, 5L);
        storage.store("BTC-USD", Interval.ONE_MINUTE, candle);

        List<Candle> result = storage.getCandles("BTC-USD", Interval.ONE_MINUTE, 1_000L, 1_000L);
        assertEquals(1, result.size());
        assertEquals(candle, result.get(0));
    }

    @Test
    void rangeQueryReturnsOnlyCandlesWithinRange() {
        storage.store("BTC-USD", Interval.ONE_MINUTE, new Candle(100L, 1, 2, 1, 2, 1));
        storage.store("BTC-USD", Interval.ONE_MINUTE, new Candle(160L, 2, 3, 1, 3, 1));
        storage.store("BTC-USD", Interval.ONE_MINUTE, new Candle(220L, 3, 4, 2, 4, 1));
        storage.store("BTC-USD", Interval.ONE_MINUTE, new Candle(280L, 4, 5, 3, 5, 1));

        List<Candle> result = storage.getCandles("BTC-USD", Interval.ONE_MINUTE, 160L, 220L);
        assertEquals(2, result.size());
        assertEquals(160L, result.get(0).time());
        assertEquals(220L, result.get(1).time());
    }

    @Test
    void emptyResultForUnknownSymbol() {
        List<Candle> result = storage.getCandles("UNKNOWN", Interval.ONE_MINUTE, 0L, 9_999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void candlesForDifferentIntervalsAreIsolated() {
        Candle candle = new Candle(1_000L, 100.0, 110.0, 90.0, 105.0, 5L);
        storage.store("BTC-USD", Interval.ONE_MINUTE, candle);

        assertEquals(1, storage.getCandles("BTC-USD", Interval.ONE_MINUTE, 0L, 9_999L).size());
        assertTrue(storage.getCandles("BTC-USD", Interval.ONE_HOUR, 0L, 9_999L).isEmpty());
    }

    @Test
    void candlesForDifferentSymbolsAreIsolated() {
        storage.store("BTC-USD", Interval.ONE_MINUTE, new Candle(1_000L, 100, 110, 90, 105, 5));
        storage.store("ETH-USD", Interval.ONE_MINUTE, new Candle(1_000L, 200, 210, 190, 205, 3));

        assertEquals(1, storage.getCandles("BTC-USD", Interval.ONE_MINUTE, 0L, 9_999L).size());
        assertEquals(1, storage.getCandles("ETH-USD", Interval.ONE_MINUTE, 0L, 9_999L).size());
    }

    @Test
    void resultIsSortedAscendingByTime() {
        storage.store("BTC-USD", Interval.ONE_SECOND, new Candle(300L, 1, 2, 1, 2, 1));
        storage.store("BTC-USD", Interval.ONE_SECOND, new Candle(100L, 2, 3, 1, 3, 1));
        storage.store("BTC-USD", Interval.ONE_SECOND, new Candle(200L, 3, 4, 2, 4, 1));

        List<Candle> result = storage.getCandles("BTC-USD", Interval.ONE_SECOND, 0L, 9_999L);
        assertEquals(3, result.size());
        assertEquals(100L, result.get(0).time());
        assertEquals(200L, result.get(1).time());
        assertEquals(300L, result.get(2).time());
    }
}
