package org.example.paymentservice.repository;

import org.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * Finds the payment associated with one provider operation.
     *
     * @param providerIdempotencyKey stable key derived from the payment command
     * @return matching payment, when the operation was persisted
     */
    Optional<Payment> findByProviderIdempotencyKey(UUID providerIdempotencyKey);
}
