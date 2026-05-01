package org.example.paymentservice.repository;

import org.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for Payment persistence operations.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
