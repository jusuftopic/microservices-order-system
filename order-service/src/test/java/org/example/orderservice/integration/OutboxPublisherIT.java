package org.example.orderservice.integration;

import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.repository.OutboxRepository;
import org.example.orderservice.service.outbox.OutboxEventScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests integration flow of publishing events to Kafka
 */
public class OutboxPublisherIT extends AbstractIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxEventScheduler scheduler;

    @BeforeEach
    public void setUp() {
        orderRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Test
    void shouldPublishOutboxEventToKafka() {

        // given
        Order order = new Order();
        order.addItem(OrderItem.builder()
                        .productId(1L)
                        .quantity(2)
                .build());
        order.setStatus(OrderStatus.CREATED);
        order.setCustomerEmail("test@test.com");

        order = orderRepository.save(order);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(order.getId());
        event.setPayload("{\"test\":\"payload\"}");
        event.setProcessed(false);
        event.setEventType("PAYMENT_REQUESTED");

        outboxRepository.save(event);

        // when
        scheduler.publishOutboxEvents();

        // then (wait for async Kafka send)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {

                    List<OutboxEvent> updated = outboxRepository.findAll();

                    assertThat(updated.get(0).getProcessed()).isTrue();
                });
    }
}
