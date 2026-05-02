package com.example.candleaggregation.model;

public record BidAskEvent(String symbol, double bid, double ask, long timestamp) {}
