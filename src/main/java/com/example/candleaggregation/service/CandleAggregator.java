package com.example.candleaggregation.service;

import com.example.candleaggregation.model.BidAskEvent;
import com.example.candleaggregation.model.Candle;
import com.example.candleaggregation.model.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class CandleAggregator {

    private static final Logger log = LoggerFactory.getLogger(CandleAggregator.class);

    private final CandleStorageService storageService;

    // ConcurrentHashMap.compute() guarantees atomicity per-key, so LiveCandle mutations are safe
    private final ConcurrentHashMap<String, LiveCandle> liveCandles = new ConcurrentHashMap<>();

    public CandleAggregator(CandleStorageService storageService) {
        this.storageService = storageService;
    }

    public void process(BidAskEvent event) {
        double midPrice = (event.bid() + event.ask()) / 2.0;
        log.debug("Tick: {} mid={} @ {}", event.symbol(), midPrice, event.timestamp());

        for (Interval interval : Interval.values()) {
            aggregateForInterval(event.symbol(), midPrice, event.timestamp(), interval);
        }
    }

    private void aggregateForInterval(String symbol, double price, long timestamp, Interval interval) {
        long windowStart = (timestamp / interval.getSeconds()) * interval.getSeconds();
        String key = symbol + ":" + interval.getLabel();

        liveCandles.compute(key, (k, current) -> {
            if (current == null) {
                log.info("Opening candle [{}/{}] @ {}", symbol, interval.getLabel(), windowStart);
                return new LiveCandle(windowStart, price);
            }
            if (windowStart > current.windowStart) {
                Candle finalized = current.toCandle();
                storageService.store(symbol, interval, finalized);
                log.info("Finalized [{}/{}] @ {}: O={} H={} L={} C={} V={}",
                    symbol, interval.getLabel(), current.windowStart,
                    finalized.open(), finalized.high(), finalized.low(), finalized.close(), finalized.volume());
                return new LiveCandle(windowStart, price);
            }
            current.update(price);
            return current;
        });
    }

    // Returns a snapshot of the currently open candle, or null if none exists for this key.
    public Candle getLiveCandle(String symbol, Interval interval) {
        LiveCandle live = liveCandles.get(symbol + ":" + interval.getLabel());
        return live != null ? live.toCandle() : null;
    }
}
