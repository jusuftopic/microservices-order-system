package org.example.orderservice.integration;

import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.events.InventoryReservedEvent;
import org.example.messagingstarter.contracts.lifecycle.OrderStatus;
import org.example.messagingstarter.inbox.repository.InboxRepository;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RepeatedBusinessOutcomeIT extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InboxRepository inboxRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        inboxRepository.deleteAll();
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void repeated_inventory_reservation_with_new_message_id_does_not_add_outbox_records() {
        Long orderId = createOrder();

        orderService.handleInventoryReserved(
                new InventoryReservedEvent(orderId, "corr-123", UUID.randomUUID()));
        orderService.handleInventoryReserved(
                new InventoryReservedEvent(orderId, "corr-123", UUID.randomUUID()));

        assertSingleReservationOutcome(orderId);
    }

    private Long createOrder() {
        Order order = Order.builder()
                .status(OrderStatus.CREATED)
                .customerEmail("customer@example.com")
                .correlationId("corr-123")
                .build();
        order.addItem(OrderItem.builder().productId(10L).quantity(1).build());
        return orderRepository.saveAndFlush(order).getId();
    }

    private void assertSingleReservationOutcome(Long orderId) {
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.INVENTORY_RESERVE_COMPLETED);
        assertThat(inboxRepository.count()).isEqualTo(2);
        assertThat(outboxRepository.findAll())
                .extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder(
                        EventConstants.EVENT_ORDER_LIFECYCLE_TRANSITIONED,
                        EventConstants.EVENT_PAYMENT_REQUESTED
                );
    }
}
