package org.example.orderservice.unit.service;

import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.commands.ReleaseInventoryCommand;
import org.example.messagingstarter.contracts.commands.RefundPaymentCommand;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.service.OrderCompensationService;
import org.example.orderservice.service.outbox.OrderOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests {@link OrderCompensationService}
 */
@ExtendWith(MockitoExtension.class)
public class OrderCompensationServiceTest {


    @Mock
    private OrderOutboxService outboxService;

    private OrderCompensationService compensationService;

    @BeforeEach
    void setUp() {
        compensationService = new OrderCompensationService(
                outboxService
        );
    }

    @Test
    void should_release_inventory_when_inventory_reserved() {

        // GIVEN
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.INVENTORY_RESERVE_COMPLETED)
                .correlationId("corr-1")
                .build();

        order.addItem(
                OrderItem.builder()
                        .productId(10L)
                        .quantity(5)
                        .build()
        );

        // WHEN
        compensationService.compensate(order);

        // THEN
        verify(outboxService).storeEvent(
                eq(1L),
                eq("ORDER"),
                eq(EventConstants.EVENT_INVENTORY_RELEASE_REQUESTED),
                any(ReleaseInventoryCommand.class)
        );

        verifyNoMoreInteractions(outboxService);
    }

    @Test
    void should_refund_and_release_inventory_when_payment_completed() {

        // GIVEN
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.PAYMENT_COMPLETED)
                .correlationId("corr-1")
                .build();

        order.addItem(
                OrderItem.builder()
                        .productId(10L)
                        .quantity(1)
                        .build()
        );

        // WHEN
        compensationService.compensate(order);

        // THEN
        verify(outboxService).storeEvent(
                eq(1L),
                eq("ORDER"),
                eq(EventConstants.EVENT_PAYMENT_REFUND_REQUESTED),
                any(RefundPaymentCommand.class)
        );

        verify(outboxService).storeEvent(
                eq(1L),
                eq("ORDER"),
                eq(EventConstants.EVENT_INVENTORY_RELEASE_REQUESTED),
                any(ReleaseInventoryCommand.class)
        );

        verify(outboxService, times(2))
                .storeEvent(
                        eq(1L),
                        eq("ORDER"),
                        anyString(),
                        any()
                );
    }

    @Test
    void should_not_publish_any_events_when_no_compensation_required() {

        // GIVEN
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.CREATED)
                .correlationId("corr-1")
                .build();

        // WHEN
        compensationService.compensate(order);

        // THEN
        verifyNoInteractions(outboxService);
    }

    @Test
    void should_not_publish_any_events_for_completed_order() {

        // GIVEN
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.COMPLETED)
                .correlationId("corr-1")
                .build();

        // WHEN
        compensationService.compensate(order);

        // THEN
        verifyNoInteractions(outboxService);
    }

}
