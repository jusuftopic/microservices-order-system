package org.example.inventoryservice.integration;

import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryCheckRequestedEvent;
import org.example.commons.event.contracts.OrderItemEvent;
import org.example.inventoryservice.entity.InventoryItem;
import org.example.inventoryservice.repository.InboxRepository;
import org.example.inventoryservice.repository.InventoryRepository;
import org.example.inventoryservice.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test validating idempotent processing of inventory events.
 *
 * <p>
 * Ensures that duplicate Kafka events with the same correlationId
 * are processed only once.
 * </p>
 */
public class IdempotencyIT extends AbstractIntegrationTest {


    @Autowired
    private KafkaTemplate<String, InventoryCheckRequestedEvent> kafkaTemplate;

    @Autowired
    private InboxRepository inboxRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private InventoryRepository inventoryRepository;


    @Test
    void shouldNotProcessSameEventTwice() {

        // GIVEN
        InventoryItem item = InventoryItem.builder()
                .productId(10L)
                .availableQuantity(10)
                .reservedQuantity(0)
                .build();

        inventoryRepository.save(item);

        InventoryCheckRequestedEvent event =
                new InventoryCheckRequestedEvent(
                        1L,
                        List.of(new OrderItemEvent(10L, 1)),
                        "same-corr"
                );

        // WHEN
        kafkaTemplate.send(EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1, "1", event);
        kafkaTemplate.send(EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1, "1", event);

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
