package org.example.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.*;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.mapper.OrderMapper;
import org.example.orderservice.repository.InboxRepository;
import org.example.orderservice.repository.OutboxRepository;
import org.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final OutboxRepository outboxRepository;
    private final InboxRepository inboxRepository;

    private final ObjectMapper objectMapper;

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
        storeOutboxEvent(
                saved,
                EVENT_INVENTORY_CHECK_REQUESTED,
                new InventoryCheckRequestedEvent(
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
            log.warn("[ORDER-SERVICE] Event {} already processed.", event.messageId());
            return;
        }

        Order order = repository.findById(event.orderId())
                .orElseThrow();

        log.info("[ORDER-SERVICE] Processing inventory success for order {}", order.getId());

        // update state
        order.setStatus(OrderStatus.INVENTORY_PROCESSING);
        repository.save(order);

        storeOutboxEvent(
                order,
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

        Order order = repository.findById(event.orderId())
                .orElseThrow();

        log.warn("[ORDER-SERVICE] Inventory failed for order {} reason {}",
                order.getId(), event.reason());

        order.setStatus(OrderStatus.FAILED);
        repository.save(order);
    }


    private void storeOutboxEvent(Order order, String eventType, Object payload) {

        final OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateType("ORDER");
        outboxEvent.setAggregateId(order.getId());
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayload(toJson(payload));
        outboxEvent.setProcessed(false);
        outboxEvent.setCreatedAt(LocalDateTime.now());

        outboxRepository.save(outboxEvent);
    }

    private Order storeOrder(final OrderRequest request, String correlationId) {
        Order order = OrderMapper.toEntity(request);
        order.setCorrelationId(correlationId);
        return repository.save(order);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.error("[ORDER-SERVICE] Failed to serialize outbox payload. Reason: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
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
