package com.example.candleaggregation.service;

import com.example.candleaggregation.model.Candle;
import com.example.candleaggregation.model.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Service
public class CandleStorageService {

    private static final Logger log = LoggerFactory.getLogger(CandleStorageService.class);

    // ConcurrentSkipListMap keeps candles sorted by timestamp, enabling O(log n) range queries
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Long, Candle>> storage =
        new ConcurrentHashMap<>();

    public void store(String symbol, Interval interval, Candle candle) {
        storage.computeIfAbsent(storageKey(symbol, interval), k -> new ConcurrentSkipListMap<>())
               .put(candle.time(), candle);
        log.debug("Stored [{}/{}] @ {}: O={} H={} L={} C={} V={}",
            symbol, interval.getLabel(), candle.time(),
            candle.open(), candle.high(), candle.low(), candle.close(), candle.volume());
    }

    public List<Candle> getCandles(String symbol, Interval interval, long from, long to) {
        ConcurrentSkipListMap<Long, Candle> map = storage.get(storageKey(symbol, interval));
        if (map == null) return Collections.emptyList();
        return new ArrayList<>(map.subMap(from, true, to, true).values());
    }

    private String storageKey(String symbol, Interval interval) {
        return symbol + ":" + interval.getLabel();
    }
}
