package org.example.paymentservice.service.provider.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Set;

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

        log.info(
                "[PAYMENT-SERVICE][STRIPE-WEBHOOK] Accepted event {} of type {}",
                event.id(),
                event.type()
        );
        return true;
    }
}
