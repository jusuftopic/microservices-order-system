package org.example.paymentservice.service.provider;

import org.example.paymentservice.dto.PaymentResultDTO;

import java.math.BigDecimal;

/**
 * Interface for different payment provider connectors
 */
public interface PaymentProvider {

    /**
     * Process payment process for an order ID
     *
     * @param orderId ID of the charging order
     * @return Payment processing result provides result of the action
     */
    PaymentResultDTO pay(Long orderId);
}
