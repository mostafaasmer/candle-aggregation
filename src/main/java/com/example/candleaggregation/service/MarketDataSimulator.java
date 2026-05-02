package com.example.candleaggregation.service;

import com.example.candleaggregation.config.KafkaConfig;
import com.example.candleaggregation.model.BidAskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class MarketDataSimulator {

    private static final Logger log = LoggerFactory.getLogger(MarketDataSimulator.class);

    private static final String[] SYMBOLS = {"BTC-USD", "ETH-USD", "SOL-USD"};

    // Per-symbol mid-prices for random-walk simulation; accessed only from the scheduler thread
    private final double[] midPrices = {65_000.0, 3_500.0, 180.0};

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public MarketDataSimulator(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 30_000)
    public void generateTick() {
        long now = System.currentTimeMillis() / 1000L;
        for (int i = 0; i < SYMBOLS.length; i++) {
            midPrices[i] *= 1.0 + ThreadLocalRandom.current().nextGaussian() * 0.0005;
            double spread = midPrices[i] * 0.0001;
            BidAskEvent event = new BidAskEvent(
                SYMBOLS[i],
                midPrices[i] - spread,
                midPrices[i] + spread,
                now
            );
            kafkaTemplate.send(KafkaConfig.BID_ASK_TOPIC, event.symbol(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish tick for {}: {}", event.symbol(), ex.getMessage());
                    } else {
                        log.debug("Published {} @ partition={} offset={}",
                            event.symbol(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    }
                });
        }
    }
}
