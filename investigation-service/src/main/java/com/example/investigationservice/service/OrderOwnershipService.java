package com.example.investigationservice.service;

import com.example.investigationservice.entity.OrderOwner;
import com.example.investigationservice.exception.InvalidOrderOwnershipEventException;
import com.example.investigationservice.repository.OrderOwnerRepository;
import com.example.investigationservice.validation.OrderOwnershipEventValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.contracts.events.OrderOwnershipAssignedEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Maintains the local order ownership projection used for access decisions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOwnershipService {

    private final OrderOwnerRepository repository;
    private final OrderOwnershipEventValidator validator;

    /**
     * Persists an ownership event idempotently with its Kafka source coordinates.
     * An existing assignment cannot be overwritten by a later event.
     *
     * @param event ownership fact emitted by the Order Service
     * @param kafkaTopic source Kafka topic
     * @param kafkaPartition source Kafka partition
     * @param kafkaOffset source Kafka offset
     * @return {@code true} when a new projection was stored, or {@code false}
     *         for a repeated delivery of the same event
     */
    @Transactional
    public boolean persist(
            OrderOwnershipAssignedEvent event,
            String kafkaTopic,
            int kafkaPartition,
            long kafkaOffset
    ) {
        validator.validate(event, kafkaTopic, kafkaPartition, kafkaOffset);

        Optional<OrderOwner> ownerByMessage = repository.findByMessageId(event.messageId());
        if (ownerByMessage.isPresent()) {
            return handleExisting(ownerByMessage.get(), event);
        }

        Optional<OrderOwner> ownerByOrder = repository.findById(event.orderId());
        if (ownerByOrder.isPresent()) {
            return handleExisting(ownerByOrder.get(), event);
        }

        try {
            repository.saveAndFlush(toOwner(event));
        } catch (DataIntegrityViolationException exception) {
            Optional<OrderOwner> concurrentlyStored = repository.findById(event.orderId());
            if (concurrentlyStored.isPresent()) {
                return handleExisting(concurrentlyStored.get(), event);
            }
            throw exception;
        }

        log.debug(
                "[INVESTIGATION-SERVICE][OWNERSHIP] Persisted owner for order {} "
                        + "from {}-{}@{} messageId {}",
                event.orderId(),
                kafkaTopic,
                kafkaPartition,
                kafkaOffset,
                event.messageId()
        );
        return true;
    }

    /**
     * Checks the local projection for an exact order/customer ownership match.
     * Missing or invalid inputs fail closed.
     *
     * @param orderId order identifier
     * @param customerId authenticated internal customer identifier
     * @return {@code true} only when the order is locally assigned to the customer
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Long orderId, UUID customerId) {
        if (orderId == null || orderId <= 0 || customerId == null) {
            return false;
        }
        return repository.existsByOrderIdAndCustomerId(orderId, customerId);
    }

    /**
     * Treats an exact redelivery as idempotent and rejects conflicting facts.
     *
     * @param stored locally persisted ownership fact
     * @param incoming ownership event being processed
     * @return {@code false} for an exact redelivery
     */
    private boolean handleExisting(
            OrderOwner stored,
            OrderOwnershipAssignedEvent incoming
    ) {
        if (stored.getOrderId().equals(incoming.orderId())
                && stored.getCustomerId().equals(incoming.customerId())
                && stored.getMessageId().equals(incoming.messageId())) {
            log.debug(
                    "[INVESTIGATION-SERVICE][OWNERSHIP] Ignoring duplicate ownership "
                            + "event {} for order {}",
                    incoming.messageId(),
                    incoming.orderId()
            );
            return false;
        }

        throw new InvalidOrderOwnershipEventException(
                "Order ownership assignment conflicts with an existing projection"
        );
    }

    /**
     * Maps the integration event and its source coordinates to the projection.
     *
     * @param event validated ownership event
     * @return immutable ownership projection
     */
    private OrderOwner toOwner(OrderOwnershipAssignedEvent event) {
        return OrderOwner.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .messageId(event.messageId())
                .occurredAt(event.occurredAt())
                .eventVersion(event.eventVersion())
                .correlationId(event.correlationId())
                .build();
    }
}
