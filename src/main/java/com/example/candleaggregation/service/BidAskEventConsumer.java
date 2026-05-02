package com.example.candleaggregation.service;

import com.example.candleaggregation.config.KafkaConfig;
import com.example.candleaggregation.model.BidAskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BidAskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BidAskEventConsumer.class);

    private final CandleAggregator aggregator;

    public BidAskEventConsumer(CandleAggregator aggregator) {
        this.aggregator = aggregator;
    }

    // 3 concurrent consumer threads
    @KafkaListener(
        topics = KafkaConfig.BID_ASK_TOPIC,
        groupId = "candle-aggregation-group",
        concurrency = "3"
    )
    public void consume(BidAskEvent event) {
        log.debug("Consumed {} bid={} ask={} @ {}", event.symbol(), event.bid(), event.ask(), event.timestamp());
        aggregator.process(event);
    }
}
