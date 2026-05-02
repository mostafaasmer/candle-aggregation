package com.example.candleaggregation.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HistoryResponse(
    String s,
    long[] t,
    double[] o,
    double[] h,
    double[] l,
    double[] c,
    long[] v,
    String errmsg
) {
    public static HistoryResponse from(List<Candle> candles) {
        int n = candles.size();
        long[] t = new long[n];
        double[] o = new double[n];
        double[] h = new double[n];
        double[] l = new double[n];
        double[] c = new double[n];
        long[] v = new long[n];

        for (int i = 0; i < n; i++) {
            Candle candle = candles.get(i);
            t[i] = candle.time();
            o[i] = candle.open();
            h[i] = candle.high();
            l[i] = candle.low();
            c[i] = candle.close();
            v[i] = candle.volume();
        }
        return new HistoryResponse("ok", t, o, h, l, c, v, null);
    }

    public static HistoryResponse error(String message) {
        return new HistoryResponse("error", null, null, null, null, null, null, message);
    }
}
