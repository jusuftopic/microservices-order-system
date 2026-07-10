package org.example.paymentservice.integration;

import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.commands.ProcessPaymentCommand;
import org.example.messagingstarter.inbox.repository.InboxRepository;
import org.junit.jupiter.api.BeforeEach;
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
public class IdempotencyIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, ProcessPaymentCommand> kafkaTemplate;

    @Autowired
    private InboxRepository inboxRepository;

    @BeforeEach
    public void setUp() {
        inboxRepository.deleteAll();
    }

    @Test
    void shouldNotProcessSameEventTwice() {
        // GIVEN
        ProcessPaymentCommand event = new ProcessPaymentCommand(
                1L, BigDecimal.ONE,
                "test", "1x1",
                UUID.randomUUID()
        );

        // WHEN
        // send twice
        kafkaTemplate.send(EventConstants.TOPIC_ORDER_PAYMENT_REQUEST_V1, event.orderId().toString(), event);
        kafkaTemplate.send(EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1, event.orderId().toString(), event);

        // THEN -> wait 10 seconds and verify repository
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {

                    // only one inbox entry expected
                    assertThat(inboxRepository.count()).isEqualTo(1);
                });
    }

}
