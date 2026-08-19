package com.example.investigationservice.repository;

import com.example.investigationservice.entity.OrderLifecycleEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Provides persistence access to the lifecycle evidence collected by the
 * Investigation Service.
 */
public interface OrderLifecycleEvidenceRepository
        extends JpaRepository<OrderLifecycleEvidence, Long> {

    /**
     * Checks whether an event has already been stored using its globally
     * unique message identifier.
     *
     * @param messageId identifier assigned to the lifecycle event
     * @return {@code true} when evidence for the message already exists
     */
    boolean existsByMessageId(UUID messageId);

    /**
     * Reads an order timeline in deterministic business-time order.
     * Message ID provides a stable tie-breaker for equal occurrence times.
     * It does not imply causal ordering between those events.
     *
     * @param orderId investigated order identifier
     * @return evidence ordered from oldest to newest
     */
    List<OrderLifecycleEvidence> findByOrderIdOrderByOccurredAtAscMessageIdAsc(Long orderId);
}
