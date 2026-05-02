package com.example.candleaggregation.controller;

import com.example.candleaggregation.model.Candle;
import com.example.candleaggregation.model.HistoryResponse;
import com.example.candleaggregation.model.Interval;
import com.example.candleaggregation.service.CandleAggregator;
import com.example.candleaggregation.service.CandleStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class HistoryController {

    private static final Logger log = LoggerFactory.getLogger(HistoryController.class);

    private final CandleStorageService storageService;
    private final CandleAggregator aggregator;

    public HistoryController(CandleStorageService storageService, CandleAggregator aggregator) {
        this.storageService = storageService;
        this.aggregator = aggregator;
    }


    @GetMapping("/history")
    public ResponseEntity<HistoryResponse> getHistory(
        @RequestParam String symbol,
        @RequestParam String interval,
        @RequestParam long from,
        @RequestParam long to
    ) {
        log.info("History: symbol={} interval={} from={} to={}", symbol, interval, from, to);

        Interval intervalEnum = Interval.fromString(interval);

        // Finalized candles from storage (sorted ascending by time)
        List<Candle> candles = new ArrayList<>(storageService.getCandles(symbol, intervalEnum, from, to));

        // Append the currently open candle if it falls within the requested range
        Candle live = aggregator.getLiveCandle(symbol, intervalEnum);
        if (live != null && live.time() >= from && live.time() <= to) {
            candles.add(live);
        }

        return ResponseEntity.ok(HistoryResponse.from(candles));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HistoryResponse> handleBadInterval(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(HistoryResponse.error(ex.getMessage()));
    }
}
