package org.example.orderservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.InventoryReleasedRequestedEvent;
import org.example.messagingstarter.contracts.OrderItemEvent;
import org.example.messagingstarter.contracts.PaymentRefundRequestedEvent;
import org.example.orderservice.entity.Order;
import org.example.orderservice.service.outbox.OrderOutboxService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for executing compensation actions for
 * orders that cannot be completed successfully.
 *
 * <p>
 * Compensation actions are triggered by the Saga Orchestrator
 * when a timeout or unrecoverable failure occurs.
 *
 * The service publishes compensation commands through the
 * transactional outbox to ensure reliable delivery via Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCompensationService {

    private final OrderOutboxService outboxService;


    /**
     * Executes compensation actions for the given order.
     *
     * @param order order requiring compensation
     */
    public void compensate(Order order) {

        log.warn(
                "[ORDER-COMPENSATION] Starting compensation for order {} in status {}",
                order.getId(),
                order.getStatus()
        );

        switch (order.getStatus()) {

            case INVENTORY_RESERVE_COMPLETED ->
                    releaseInventory(order);

            case PAYMENT_COMPLETED ->
                    compensatePaymentCompleted(order);

            default -> log.info(
                    "[ORDER-COMPENSATION] No compensation required for order {} in status {}",
                    order.getId(),
                    order.getStatus()
            );
        }
    }


    /**
     * Compensates an order after payment was already completed.
     *
     * <p>
     * Since the customer has already been charged,
     * both payment refund and inventory release are required.
     * </p>
     *
     * @param order order requiring compensation
     */
    private void compensatePaymentCompleted(Order order) {

        log.warn(
                "[ORDER-COMPENSATION] Executing payment refund and inventory release for order {}",
                order.getId()
        );

        refundPayment(order);
        releaseInventory(order);
    }

    /**
     * Publishes a refund request.
     *
     * @param order order requiring a refund
     */
    private void refundPayment(Order order) {

        log.info(
                "[ORDER-COMPENSATION] Publishing payment refund request for order {}",
                order.getId()
        );

        outboxService.storeEvent(
                order.getId(),
                "ORDER",
                EventConstants.EVENT_PAYMENT_REFUND_REQUESTED,
                new PaymentRefundRequestedEvent(
                        order.getId(),
                        order.getCorrelationId(),
                        UUID.randomUUID()
                )
        );
    }

    /**
     * Publishes an inventory release request.
     *
     * @param order order with reserved inventory
     */
    private void releaseInventory(Order order) {

        log.info(
                "[ORDER-COMPENSATION] Publishing inventory release request for order {}",
                order.getId()
        );

        outboxService.storeEvent(
                order.getId(),
                "ORDER",
                EventConstants.EVENT_INVENTORY_RELEASE_REQUESTED,
                new InventoryReleasedRequestedEvent(
                        order.getId(),
                        order.getItems()
                                .stream()
                                .map(i -> new OrderItemEvent(
                                        i.getProductId(),
                                        i.getQuantity()
                                ))
                                .toList(),
                        order.getCorrelationId(),
                        UUID.randomUUID()
                )
        );
    }
}
