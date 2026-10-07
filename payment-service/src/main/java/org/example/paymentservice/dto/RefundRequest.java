package org.example.paymentservice.dto;

/**
 * Provider-neutral request for refunding a completed payment.
 *
 * @param refundOperationId internal durable refund identifier
 * @param orderId order associated with the payment
 * @param providerPaymentId provider identifier of the completed payment
 * @param providerRefundId provider refund identifier during reconciliation
 * @param idempotencyKey stable key used for provider-side deduplication
 */
public record RefundRequest(
        Long refundOperationId,
        Long orderId,
        String providerPaymentId,
        String providerRefundId,
        String idempotencyKey
) {
}
