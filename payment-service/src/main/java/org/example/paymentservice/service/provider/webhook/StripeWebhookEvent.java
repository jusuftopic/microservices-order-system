package org.example.paymentservice.service.provider.webhook;

import com.stripe.model.PaymentIntent;

/**
 * Authenticated Stripe PaymentIntent observation required by payment handling.
 *
 * @param id unique Stripe event identifier
 * @param type Stripe event type
 * @param paymentIntent authenticated PaymentIntent snapshot
 */
public record StripeWebhookEvent(
        String id,
        String type,
        PaymentIntent paymentIntent
) {
}
