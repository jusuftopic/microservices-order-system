package org.example.paymentservice.dto;

import org.example.paymentservice.enums.RefundProviderStatus;

/**
 * Provider-neutral result of a refund request.
 *
 * @param status provider-neutral refund outcome
 * @param providerRefundId provider identifier of the refund
 * @param failureReason failure or pending reason when not completed
 */
public record RefundResult(
        RefundProviderStatus status,
        String providerRefundId,
        String failureReason
) {
}
