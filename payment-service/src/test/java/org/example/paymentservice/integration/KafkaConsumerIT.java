package org.example.paymentservice.integration;

import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.PaymentRequestedEvent;
import org.example.paymentservice.repository.InboxRepository;
import org.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests ensure proper storing Kafka topics in the system
 */
public class KafkaConsumerIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, PaymentRequestedEvent> kafkaTemplate;

    @Autowired
    private InboxRepository inboxRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldProcessPaymentFromKafka() {

        // given
        PaymentRequestedEvent event = new PaymentRequestedEvent(
                1L, BigDecimal.ONE,
                "test", "1x1", UUID.randomUUID()
        );

        // when
        kafkaTemplate.send(
                EventConstants.TOPIC_ODER_PAYMENT_REQUEST_V1,
                event.orderId().toString(),
                event
        );

        // then (wait for async Kafka consumer)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {

                    assertThat(inboxRepository.count()).isEqualTo(1);
                    assertThat(paymentRepository.findAll()).hasSize(1);
                });
    }
}
