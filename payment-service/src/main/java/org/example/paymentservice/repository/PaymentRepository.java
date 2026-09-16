package org.example.paymentservice.repository;

import org.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment persistence operations.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment entity by its unique order id
     *
     * @param orderId Order ID of the initiated payment
     * @return {@link Payment} entity if found
     */
    Payment findByOrderId(Long orderId);

    /**
     * Finds and locks the payment associated with one provider operation.
     * The lock serializes synchronous and webhook observations while the local
     * payment and outbox records are updated. No provider call is made while
     * the lock is held.
     *
     * @param providerIdempotencyKey stable key derived from the payment command
     * @return matching locked payment, when the operation was persisted
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByProviderIdempotencyKey(UUID providerIdempotencyKey);
}
