package com.example.candleaggregation.service;

import com.example.candleaggregation.model.Candle;

class LiveCandle {

    final long windowStart;
    double open;
    double high;
    double low;
    double close;
    long volume;

    LiveCandle(long windowStart, double price) {
        this.windowStart = windowStart;
        this.open = price;
        this.high = price;
        this.low = price;
        this.close = price;
        this.volume = 1;
    }

    void update(double price) {
        if (price > high) high = price;
        if (price < low) low = price;
        close = price;
        volume++;
    }

    Candle toCandle() {
        return new Candle(windowStart, open, high, low, close, volume);
    }
}
