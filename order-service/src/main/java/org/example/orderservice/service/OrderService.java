package org.example.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.*;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.mapper.OrderMapper;
import org.example.orderservice.repository.InboxRepository;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.outbox.OrderOutboxService;
import org.example.orderservice.service.workflow.OrderWorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.example.commons.event.EventConstants.EVENT_INVENTORY_CHECK_REQUESTED;


/**
 * Service layer responsible for handling business logic related to Orders.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository repository;
    private final InboxRepository inboxRepository;
    private final OrderOutboxService outboxService;
    private final OrderWorkflowService workflowService;

    /**
     * Creates a new initial Order
     *
     * @return persisted Order response
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("[ORDER-SERVICE] Received new order from customer {}. Total items {}",
                request.customerEmail(), request.items().size());

        final String correlationId = UUID.randomUUID().toString();
        final Order saved = storeOrder(request, correlationId);
        final UUID messageId = UUID.randomUUID();

        // store outbox event
        outboxService.storeEvent(
                saved.getId(),
                "ORDER",
                EVENT_INVENTORY_CHECK_REQUESTED,
                new InventoryReserveRequestedEvent(
                        saved.getId(),
                        saved.getItems().stream()
                                .map(item -> new OrderItemEvent(
                                        item.getProductId(),
                                        item.getQuantity()
                                ))
                                .toList(),
                        correlationId,
                        messageId
                )
        );

        /* 3. return order details */
        log.info("[ORDER-SERVICE] Order {} successfully created. Status: {}", saved.getId(), saved.getStatus());
        return OrderMapper.toResponse(saved);
    }

    /**
     * Handles successful inventory reservation.
     *
     * <p>
     * Updates order status and triggers payment step via outbox event.
     * </p>
     *
     * @param event inventory reserved event
     */
    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {
        int inserted = inboxRepository.insertIfNotExists(event.messageId());
        if (inserted == 0) {
            logAlreadyProcessed(event.messageId());
            return;
        }

        Order order = workflowService.updateStatus(event.orderId(), OrderStatus.INVENTORY_RESERVE_COMPLETED);

        outboxService.storeEvent(
                order.getId(),
                "ORDER",
                EventConstants.EVENT_PAYMENT_REQUESTED,
                new PaymentRequestedEvent(
                        order.getId(),
                        calculateAmount(order),
                        order.getCustomerEmail(),
                        event.correlationId(),
                        UUID.randomUUID()
                )
        );
    }

    /**
     * Handles inventory failure scenario.
     *
     * <p>
     * Marks order as failed.
     * </p>
     *
     * @param event inventory failed event
     */
    @Transactional
    public void handleInventoryFailed(InventoryFailedEvent event) {
        final Order order = workflowService.updateStatus(event.orderId(), OrderStatus.INVENTORY_RESERVE_FAILED);
        log.warn("[ORDER-SERVICE] Inventory failed for order {} reason {}. No further processing.",
                order.getId(), event.reason());
    }

    /**
     * Handles successful payment.
     *
     * <p>
     * Updates order status and triggers inventory commit step via outbox event.
     * </p>
     *
     * @param event payment complete event
     */
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        int inserted = inboxRepository.insertIfNotExists(event.messageId());

        if (inserted == 0) {
            logAlreadyProcessed(event.messageId());
            return;
        }

        Order order = workflowService.updateStatus(event.orderId(), OrderStatus.PAYMENT_COMPLETED);

        log.info("[ORDER-SERVICE] Payment completed for order {}", order.getId());

        outboxService.storeEvent(
                order.getId(),
                "ORDER",
                EventConstants.EVENT_INVENTORY_COMMIT_REQUESTED,
                new InventoryCommitEvent(
                        order.getId(),
                        order.getItems().stream()
                                .map(o -> new OrderItemEvent(o.getProductId(), o.getQuantity()))
                                .toList(),
                        event.correlationId(),
                        UUID.randomUUID()
                )
        );
    }

    /**
     * Handles failed payment.
     *
     * <p>
     * Updates order status and triggers inventory release step via outbox event.
     * </p>
     *
     * @param event payment failed event
     */
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {

        int inserted = inboxRepository.insertIfNotExists(event.messageId());

        if (inserted == 0) {
            logAlreadyProcessed(event.messageId());
            return;
        }

        Order order = workflowService.updateStatus(
                event.orderId(),
                OrderStatus.PAYMENT_FAILED
        );

        log.warn(
                "[ORDER-SERVICE] Payment failed for order {} reason {}",
                order.getId(),
                event.reason()
        );

        outboxService.storeEvent(
                order.getId(),
                "ORDER",
                EventConstants.EVENT_INVENTORY_RELEASE_REQUESTED,
                new InventoryReleasedRequestedEvent(
                        order.getId(),
                        order.getItems().stream()
                                .map(o -> new OrderItemEvent(
                                        o.getProductId(),
                                        o.getQuantity()
                                ))
                                .toList(),
                        event.correlationId(),
                        UUID.randomUUID()
                )
        );
    }

    private Order storeOrder(final OrderRequest request, String correlationId) {
        Order order = OrderMapper.toEntity(request);
        order.setCorrelationId(correlationId);
        return repository.save(order);
    }

        private void logAlreadyProcessed(UUID messageId) {
            log.warn("[ORDER-SERVICE] Event {} already processed.", messageId);
        }

    private BigDecimal calculateAmount(Order order) {
        return order.getItems().stream()
                .map(item -> BigDecimal.valueOf(item.getQuantity()).multiply(BigDecimal.TEN))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Retrieves an order by its ID.
     *
     * @param id order identifier
     * @return found Order
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return repository.findById(id)
                .map(OrderMapper::toResponse)
                .orElse(null);
    }
}
