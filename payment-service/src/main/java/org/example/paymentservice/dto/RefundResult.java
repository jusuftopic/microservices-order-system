package org.example.paymentservice.dto;

/**
 * Provider-neutral result of a refund request.
 *
 * @param succeeded whether the provider confirmed refund completion
 * @param providerRefundId provider identifier of the refund
 * @param failureReason failure or pending reason when not completed
 */
public record RefundResult(
        boolean succeeded,
        String providerRefundId,
        String failureReason
) {
}
