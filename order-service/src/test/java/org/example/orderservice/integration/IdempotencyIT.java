package org.example.orderservice.integration;

import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryReservedEvent;
import org.example.orderservice.entity.Order;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.repository.InboxRepository;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.repository.OutboxRepository;
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

    @Test
    void shouldNotProcessSameEventTwice() {
        // GIVEN

        final InventoryReservedEvent event = new InventoryReservedEvent(
                1L, "test", UUID.randomUUID()
        );

        final Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CREATED);
        order.setCustomerEmail("test");

        orderRepository.save(order);

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
