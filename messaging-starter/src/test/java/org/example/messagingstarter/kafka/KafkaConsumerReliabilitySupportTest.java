package org.example.messagingstarter.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaConsumerReliabilitySupportTest {

    @Test
    void terminalDeadLetterHandlerRecoversWithoutAnotherPublication() {
        DefaultErrorHandler errorHandler =
                KafkaConsumerReliabilitySupport.terminalDeadLetterErrorHandler();
        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("service.dlq", 0, 12L, "order-42", new Object());

        boolean recovered = errorHandler.handleOne(
                new IllegalStateException("observer failed"),
                record,
                null,
                null
        );

        assertTrue(recovered);
    }
}
