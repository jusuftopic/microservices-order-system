package com.example.investigationservice.repository;

import com.example.investigationservice.entity.OrderOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides persistence access to the local order ownership projection.
 */
public interface OrderOwnerRepository extends JpaRepository<OrderOwner, Long> {

    /**
     * Finds the ownership fact stored for a message.
     *
     * @param messageId ownership event identifier
     * @return matching ownership projection when present
     */
    Optional<OrderOwner> findByMessageId(UUID messageId);

    /**
     * Checks whether an order belongs to a particular customer.
     *
     * @param orderId order identifier
     * @param customerId internal customer identifier
     * @return {@code true} only when the local projection matches both values
     */
    boolean existsByOrderIdAndCustomerId(Long orderId, UUID customerId);
}
