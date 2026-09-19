package org.example.paymentservice.mapper;

import com.stripe.model.PaymentIntent;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.enums.PaymentProviderStatus;
import org.springframework.stereotype.Component;

/**
 * Translates Stripe PaymentIntent snapshots into the provider-neutral payment result.
 */
@Component
public class StripePaymentResultMapper {

    /**
     * Maps the current Stripe PaymentIntent state into the internal provider contract.
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
}
