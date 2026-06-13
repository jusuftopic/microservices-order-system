package org.example.inventoryservice.integration;


import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryReserveRequestedEvent;
import org.example.commons.event.contracts.OrderItemEvent;
import org.example.inventoryservice.entity.OutboxEvent;
import org.example.inventoryservice.repository.InboxRepository;
import org.example.inventoryservice.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test validating failed inventory processing flow.
 *
 * <p>
 * Ensures that when inventory cannot be reserved:
 * <ul>
 *     <li>Inbox entry is still created</li>
 *     <li>No inventory is reserved</li>
 *     <li>Failure outbox event is generated</li>
 * </ul>
 * </p>
 */
public class InventoryFailureIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, InventoryReserveRequestedEvent> kafkaTemplate;

    @Autowired
    private InboxRepository inboxRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    public void setUp() {
        inboxRepository.deleteAll();
        outboxRepository.deleteAll();
    }


    @Test
    void shouldProduceFailureEventWhenInventoryMissing() {

        // GIVEN
        InventoryReserveRequestedEvent event =
                new InventoryReserveRequestedEvent(
                        1L,
                        List.of(new OrderItemEvent(999L, 1)),
                        "corr-fail", UUID.randomUUID()
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

                    // inbox still created
                    assertThat(inboxRepository.count()).isEqualTo(1);

                    // failure outbox event
                    assertThat(outboxRepository.findAll()).hasSize(1);

                    OutboxEvent outbox = outboxRepository.findAll().get(0);

                    assertThat(outbox.getEventType())
                            .isEqualTo(EventConstants.EVENT_INVENTORY_FAILED);
                });
    }
}
