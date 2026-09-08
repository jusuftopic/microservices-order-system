package org.example.paymentservice.service.provider.clients;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.enums.PaymentProviderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

/**
 * Stripe implementation of the payment-provider boundary.
 */
@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
@RequiredArgsConstructor
@Slf4j
public class StripePaymentClient implements PaymentClient {

    private final StripeClient stripeClient;

    @Value("${app.payment.stripe.test-payment-method}")
    private String paymentMethod;

    /**
     * Creates and confirms one PaymentIntent for an order. The command ID is
     * propagated as Stripe's idempotency key so a retried call cannot create a
     * second intent for the same operation.
     */
    @Override
    public PaymentResultDTO pay(PaymentRequest request) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(toMinorUnits(request))
                .setCurrency(request.currency().toLowerCase(Locale.ROOT))
                .setPaymentMethod(paymentMethod)
                .setConfirm(true)
                .addPaymentMethodType("card")
                .putMetadata("payment_id", request.paymentId().toString())
                .putMetadata("order_id", request.orderId().toString())
                .putMetadata("correlation_id", request.correlationId())
                .build();
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(request.idempotencyKey())
                .build();

        try {
            PaymentIntent intent = stripeClient.v1()
                    .paymentIntents()
                    .create(params, options);
            PaymentResultDTO result = toResult(intent);
            log.info(
                    "[PAYMENT-PROVIDER][STRIPE] PaymentIntent {} for order {} reached state {}",
                    intent.getId(),
                    request.orderId(),
                    result.status()
            );
            return result;
        } catch (StripeException exception) {
            throw new StripePaymentProviderException(
                    "Stripe rejected or could not process the PaymentIntent request",
                    exception
            );
        }
    }

    private long toMinorUnits(PaymentRequest request) {
        Currency currency = Currency.getInstance(request.currency().toUpperCase(Locale.ROOT));
        int fractionDigits = currency.getDefaultFractionDigits();
        return request.amount()
                .setScale(fractionDigits, RoundingMode.UNNECESSARY)
                .movePointRight(fractionDigits)
                .longValueExact();
    }

    private PaymentResultDTO toResult(PaymentIntent intent) {
        PaymentProviderStatus status = switch (intent.getStatus()) {
            case "succeeded" -> PaymentProviderStatus.SUCCEEDED;
            case "processing", "requires_confirmation" ->
                    PaymentProviderStatus.PROCESSING;
            case "requires_action" -> PaymentProviderStatus.REQUIRES_ACTION;
            case "requires_payment_method" -> PaymentProviderStatus.FAILED;
            case "canceled" -> PaymentProviderStatus.CANCELED;
            default -> throw new IllegalStateException(
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
