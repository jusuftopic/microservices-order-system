package org.example.paymentservice.service.provider.webhook;

import com.stripe.model.PaymentIntent;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.service.PaymentService;
import org.example.paymentservice.mapper.StripeResultMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Defines the Stripe event types accepted by the payment integration.
 * Payment mutation is introduced by the subsequent idempotent-processing step.
 */
@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
@Slf4j
public class StripeWebhookHandler {

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            "payment_intent.succeeded",
            "payment_intent.payment_failed",
            "payment_intent.processing",
            "payment_intent.canceled"
    );

    private final PaymentService paymentService;
    private final StripeResultMapper resultMapper;

    public StripeWebhookHandler(
            PaymentService paymentService,
            StripeResultMapper resultMapper
    ) {
        this.paymentService = paymentService;
        this.resultMapper = resultMapper;
    }

    /**
     * Accepts supported authenticated events and ignores unrelated event types.
     *
     * @param event authenticated Stripe event
     */
    public boolean handle(StripeWebhookEvent event) {
        if (!SUPPORTED_EVENT_TYPES.contains(event.type())) {
            log.debug(
                    "[PAYMENT-SERVICE][STRIPE-WEBHOOK] Ignoring unsupported event {} of type {}",
                    event.id(),
                    event.type()
            );
            return false;
        }

        PaymentIntent intent = event.paymentIntent();
        if (intent == null || intent.getId() == null) {
            throw new IllegalArgumentException(
                    "Supported Stripe event does not contain a PaymentIntent snapshot"
            );
        }

        Long paymentId = parsePaymentId(intent);
        UUID idempotencyKey = parseIdempotencyKey(intent);
        paymentService.finalizePayment(
                paymentId,
                idempotencyKey,
                resultMapper.toResult(intent)
        );

        log.info(
                "[PAYMENT-SERVICE][STRIPE-WEBHOOK] Applied event {} of type {}",
                event.id(),
                event.type()
        );
        return true;
    }

    private UUID parseIdempotencyKey(PaymentIntent intent) {
        String idempotencyKey = intent.getMetadata().get("idempotency_key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Stripe PaymentIntent does not contain idempotency_key metadata"
            );
        }
        try {
            return UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Stripe idempotency_key metadata is not a valid UUID",
                    exception
            );
        }
    }

    private Long parsePaymentId(PaymentIntent intent) {
        String paymentId = intent.getMetadata().get("payment_id");
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Stripe PaymentIntent does not contain payment_id metadata"
            );
        }
        try {
            return Long.valueOf(paymentId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Stripe payment_id metadata is not a valid payment identifier",
                    exception
            );
        }
    }

}
