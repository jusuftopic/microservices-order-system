package org.example.orderservice.integration;

import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryReservedEvent;
import org.example.orderservice.entity.Order;
import org.example.orderservice.enums.OrderStatus;
import org.example.messagingstarter.inbox.repository.InboxRepository;
import org.example.orderservice.repository.OrderRepository;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test validating idempotent processing of inventory events.
 *
 * <p>
 * Ensures that duplicate Kafka events with the same messageId
 * are processed only once.
 * </p>
 */
public class IdempotencyIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, InventoryReservedEvent> kafkaTemplate;

    @Autowired
    private InboxRepository inboxRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    public void setUp() {
        inboxRepository.deleteAll();
        orderRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Test
    void shouldNotProcessSameEventTwice() {
        // GIVEN
        final Order order = new Order();
        order.setStatus(OrderStatus.CREATED);
        order.setCustomerEmail("test");

        final Order stored = orderRepository.save(order);

        final InventoryReservedEvent event = new InventoryReservedEvent(
                stored.getId(), "test", UUID.randomUUID()
        );

        // WHEN
        kafkaTemplate.send(EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1, "1", event);
        kafkaTemplate.send(EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1, "1", event);

        // THEN
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {

                    // only one inbox entry
                    assertThat(inboxRepository.count()).isEqualTo(1);

                    // only one outbox event
                    assertThat(outboxRepository.count()).isEqualTo(1);
                });
    }
}
