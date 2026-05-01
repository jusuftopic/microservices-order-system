package org.example.paymentservice.repository;

import org.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
