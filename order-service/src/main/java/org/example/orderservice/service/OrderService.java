package org.example.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.PaymentRequestedEvent;
import org.example.commons.exception.types.NotFoundException;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.mapper.OrderMapper;
import org.example.orderservice.repository.OutboxRepository;
import org.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.example.commons.event.EventConstants.EVENT_PAYMENT_REQUESTED;


/**
 * Service layer responsible for handling business logic related to Orders.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository repository;
    private final OutboxRepository outboxRepository;

    private final ObjectMapper objectMapper;

    /**
     * Creates a new initial Order
     *
     * @return persisted Order response
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("[ORDER-SERVICE] Received new order from customer {}. Total amount {}",
                request.customerEmail(), request.amount());

        final Order saved = storeOrder(request);
        storeOutboxEvent(saved);

        /* 3. return order details */
        log.info("[ORDER-SERVICE] Order {} successfully created. Status: {}", saved.getId(), saved.getStatus());
        return OrderMapper.toResponse(saved);
    }

    private void storeOutboxEvent(Order saved) {
        final UUID eventId = UUID.randomUUID();

        final OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(eventId);
        outboxEvent.setAggregateType("ORDER");
        outboxEvent.setAggregateId(saved.getId());
        outboxEvent.setEventType(EVENT_PAYMENT_REQUESTED);
        outboxEvent.setPayload(toJson(
                new PaymentRequestedEvent(
                        eventId,
                        saved.getId(),
                        saved.getAmount(),
                        saved.getCustomerEmail()
                )
        ));
        outboxEvent.setProcessed(false);
        outboxEvent.setCreatedAt(LocalDateTime.now());

        outboxRepository.save(outboxEvent);
    }

    private Order storeOrder(final OrderRequest request) {
        Order order = OrderMapper.toEntity(request);
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
