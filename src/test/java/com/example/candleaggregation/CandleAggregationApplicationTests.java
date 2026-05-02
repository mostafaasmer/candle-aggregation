package com.example.candleaggregation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest
@EmbeddedKafka(
    partitions = 3,
    topics = {"bid-ask-events"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class CandleAggregationApplicationTests {

    @Test
    void contextLoads() {
    }

}
