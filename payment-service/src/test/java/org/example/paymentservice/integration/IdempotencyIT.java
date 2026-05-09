package org.example.paymentservice.integration;

import org.example.commons.event.EventConstants;
import org.example.commons.event.PaymentRequestedEvent;
import org.example.paymentservice.repository.InboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests ensure no duplicated payment processing in the system
 */
public class IdempotencyIT extends AbstractIntegrationTest{

    @Autowired
    private KafkaTemplate<String, PaymentRequestedEvent> kafkaTemplate;

    @Autowired
    private InboxRepository inboxRepository;

    @Test
    void shouldNotProcessSameEventTwice() {
        // GIVEN
        UUID eventId = UUID.randomUUID();

        PaymentRequestedEvent event = new PaymentRequestedEvent(
                eventId,
                1L,
                BigDecimal.TEN,
                "test@test.com"
        );

        // WHEN
        // send twice
        kafkaTemplate.send(EventConstants.TOPIC_PAYMENT_REQUESTED_V1, event.orderId().toString(), event);
        kafkaTemplate.send(EventConstants.TOPIC_PAYMENT_REQUESTED_V1, event.orderId().toString(), event);

        // THEN -> wait 10 seconds and verify repository
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {

                    // only one inbox entry expected
                    assertThat(inboxRepository.count()).isEqualTo(1);
                });
    }

}
