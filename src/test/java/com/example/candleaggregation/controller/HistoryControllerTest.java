package com.example.candleaggregation.controller;

import com.example.candleaggregation.model.Candle;
import com.example.candleaggregation.model.Interval;
import com.example.candleaggregation.service.CandleAggregator;
import com.example.candleaggregation.service.CandleStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HistoryControllerTest {

    @Mock
    private CandleStorageService storageService;

    @Mock
    private CandleAggregator aggregator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HistoryController controller = new HistoryController(storageService, aggregator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void returnsOkWithCandleData() throws Exception {
        List<Candle> candles = List.of(
            new Candle(1_620_000_000L, 29_500.0, 29_510.0, 29_490.0, 29_505.0, 10L)
        );
        when(storageService.getCandles("BTC-USD", Interval.ONE_MINUTE, 1_620_000_000L, 1_620_000_600L))
            .thenReturn(candles);
        when(aggregator.getLiveCandle("BTC-USD", Interval.ONE_MINUTE)).thenReturn(null);

        mockMvc.perform(get("/history")
                .param("symbol", "BTC-USD")
                .param("interval", "1m")
                .param("from", "1620000000")
                .param("to", "1620000600"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.s").value("ok"))
            .andExpect(jsonPath("$.t[0]").value(1_620_000_000L))
            .andExpect(jsonPath("$.o[0]").value(29_500.0))
            .andExpect(jsonPath("$.h[0]").value(29_510.0))
            .andExpect(jsonPath("$.l[0]").value(29_490.0))
            .andExpect(jsonPath("$.c[0]").value(29_505.0))
            .andExpect(jsonPath("$.v[0]").value(10));
    }

    @Test
    void returnsErrorForUnknownInterval() throws Exception {
        mockMvc.perform(get("/history")
                .param("symbol", "BTC-USD")
                .param("interval", "3m")
                .param("from", "1620000000")
                .param("to", "1620000600"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.s").value("error"))
            .andExpect(jsonPath("$.errmsg").exists());
    }

    @Test
    void includesLiveCandleWhenWithinRange() throws Exception {
        Candle stored = new Candle(1_620_000_000L, 29_500.0, 29_510.0, 29_490.0, 29_505.0, 10L);
        Candle live   = new Candle(1_620_000_060L, 29_505.0, 29_515.0, 29_495.0, 29_510.0,  5L);
        when(storageService.getCandles("BTC-USD", Interval.ONE_MINUTE, 1_620_000_000L, 1_620_000_600L))
            .thenReturn(List.of(stored));
        when(aggregator.getLiveCandle("BTC-USD", Interval.ONE_MINUTE)).thenReturn(live);

        mockMvc.perform(get("/history")
                .param("symbol", "BTC-USD")
                .param("interval", "1m")
                .param("from", "1620000000")
                .param("to", "1620000600"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.s").value("ok"))
            .andExpect(jsonPath("$.t.length()").value(2));
    }

    @Test
    void excludesLiveCandleOutsideRange() throws Exception {
        Candle live = new Candle(1_620_001_000L, 29_505.0, 29_515.0, 29_495.0, 29_510.0, 5L);
        when(storageService.getCandles("BTC-USD", Interval.ONE_MINUTE, 1_620_000_000L, 1_620_000_600L))
            .thenReturn(Collections.emptyList());
        when(aggregator.getLiveCandle("BTC-USD", Interval.ONE_MINUTE)).thenReturn(live);

        mockMvc.perform(get("/history")
                .param("symbol", "BTC-USD")
                .param("interval", "1m")
                .param("from", "1620000000")
                .param("to", "1620000600"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.s").value("ok"))
            .andExpect(jsonPath("$.t.length()").value(0));
    }

    @Test
    void returnsEmptyArraysWhenNoData() throws Exception {
        when(storageService.getCandles("BTC-USD", Interval.ONE_SECOND, 1_000L, 9_000L))
            .thenReturn(Collections.emptyList());
        when(aggregator.getLiveCandle("BTC-USD", Interval.ONE_SECOND)).thenReturn(null);

        mockMvc.perform(get("/history")
                .param("symbol", "BTC-USD")
                .param("interval", "1s")
                .param("from", "1000")
                .param("to", "9000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.s").value("ok"))
            .andExpect(jsonPath("$.t.length()").value(0));
    }
}
