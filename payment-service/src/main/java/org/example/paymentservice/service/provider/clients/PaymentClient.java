package org.example.paymentservice.service.provider.clients;

import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.dto.RefundRequest;
import org.example.paymentservice.dto.RefundResult;

/**
 * Unified interface for different payment providers connected to the system
 *
 * Note: Underlying client MUST include timeout options (Alternative: Consider using TimeLimiter)
 */
public interface PaymentClient {

    /**
     * Initiate payment process towards 3rd party payment system
     *
     * @param request provider-neutral payment request
     * @return The result of the payment process
     */
    PaymentResultDTO pay(PaymentRequest request);

    /**
     * Refunds a previously completed provider payment.
     *
     * @param request provider-neutral refund request
     * @return provider-neutral refund result
     */
    RefundResult refund(RefundRequest request);
}
