package org.example.paymentservice.mapper;

import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.dto.RefundResult;
import org.example.paymentservice.enums.PaymentProviderStatus;
import org.springframework.stereotype.Component;

/**
 * Translates Stripe responses into provider-neutral payment contracts.
 */
@Component
public class StripeResultMapper {

    /**
     * Maps a Stripe PaymentIntent into the internal payment result.
     *
     * @param intent Stripe PaymentIntent returned synchronously or by a webhook
     * @return provider-neutral payment result
     */
    public PaymentResultDTO toResult(PaymentIntent intent) {
        PaymentProviderStatus status = switch (intent.getStatus()) {
            case "succeeded" -> PaymentProviderStatus.SUCCEEDED;
            case "processing", "requires_confirmation" ->
                    PaymentProviderStatus.PROCESSING;
            case "requires_action" -> PaymentProviderStatus.REQUIRES_ACTION;
            case "requires_payment_method" -> PaymentProviderStatus.FAILED;
            case "canceled" -> PaymentProviderStatus.CANCELED;
            default -> throw new IllegalArgumentException(
                    "Unsupported Stripe PaymentIntent status: " + intent.getStatus()
            );
        };
        String failureReason = intent.getLastPaymentError() == null
                ? null : intent.getLastPaymentError().getCode();
        String nextActionType = intent.getNextAction() == null
                ? null : intent.getNextAction().getType();

        return new PaymentResultDTO(
                status,
                intent.getId(),
                failureReason,
                nextActionType,
                "STRIPE"
        );
    }

    /**
     * Maps a Stripe Refund into the internal refund result.
     *
     * @param refund Stripe refund snapshot
     * @return provider-neutral refund result
     */
    public RefundResult toResult(Refund refund) {
        boolean succeeded = "succeeded".equals(refund.getStatus());
        String reason = switch (refund.getStatus()) {
            case "succeeded" -> null;
            case "pending" -> refund.getPendingReason() == null
                    ? "REFUND_PENDING"
                    : refund.getPendingReason();
            case "requires_action" -> "REFUND_REQUIRES_ACTION";
            case "canceled" -> refund.getFailureReason() == null
                    ? "REFUND_CANCELED"
                    : refund.getFailureReason();
            case "failed" -> refund.getFailureReason() == null
                    ? "REFUND_FAILED"
                    : refund.getFailureReason();
            default -> "UNSUPPORTED_REFUND_STATUS_" + refund.getStatus();
        };
        return new RefundResult(succeeded, refund.getId(), reason);
    }
}
