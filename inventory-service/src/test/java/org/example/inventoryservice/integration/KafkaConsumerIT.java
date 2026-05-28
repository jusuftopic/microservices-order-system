package org.example.inventoryservice.integration;

import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryCheckRequestedEvent;
import org.example.commons.event.contracts.OrderItemEvent;
import org.example.inventoryservice.entity.InventoryItem;
import org.example.inventoryservice.entity.OutboxEvent;
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
 * Integration test validating the main Inventory processing flow.
 *
 * <p>
 * Verifies end-to-end behavior:
 * <ul>
 *     <li>Kafka event is consumed by listener</li>
 *     <li>Inbox entry is created (idempotency)</li>
 *     <li>Inventory is reserved</li>
 *     <li>Outbox event is created</li>
 * </ul>
 * </p>
 */
public class KafkaConsumerIT extends AbstractIntegrationTest {


    @Autowired
    private KafkaTemplate<String, InventoryCheckRequestedEvent> kafkaTemplate;

    @Autowired
    private InboxRepository inboxRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void shouldProcessInventoryAndCreateOutboxEvent() {

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
                        List.of(new OrderItemEvent(10L, 2)),
                        "corr-1"
                );

        // WHEN
        kafkaTemplate.send(
                EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1,
                "1",
                event
        );

        // THEN
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {

                    // inbox created
                    assertThat(inboxRepository.count()).isEqualTo(1);

                    // inventory updated
                    InventoryItem updated =
                            inventoryRepository.findById(10L).orElseThrow();

                    assertThat(updated.getAvailableQuantity()).isEqualTo(8);
                    assertThat(updated.getReservedQuantity()).isEqualTo(2);

                    // outbox event created
                    assertThat(outboxRepository.findAll()).hasSize(1);

                    OutboxEvent outbox = outboxRepository.findAll().get(0);

                    assertThat(outbox.getEventType())
                            .isEqualTo(EventConstants.EVENT_INVENTORY_RESERVED);
                });
    }
}
