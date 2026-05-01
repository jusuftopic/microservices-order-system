package org.example.paymentservice.service.provider.clients;

import org.example.paymentservice.dto.PaymentResultDTO;

import java.util.UUID;

/**
 * Unified interface for different payment providers connected to the system
 */
public interface PaymentClient {

    /**
     * Initiate payment process towards 3rd party payment system
     *
     * @param orderId Identifier of the order
     * @param idempotencyKey Key sent to 3rd party payment service ensures reliable
     *                       track of already processed events
     * @return The result of the payment process
     */
    PaymentResultDTO pay(Long orderId, UUID idempotencyKey);
}
