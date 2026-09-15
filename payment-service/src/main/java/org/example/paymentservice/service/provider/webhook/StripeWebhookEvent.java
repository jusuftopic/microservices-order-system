package org.example.paymentservice.service.provider.webhook;

/**
 * Authenticated Stripe event information required by the webhook boundary.
 *
 * @param id unique Stripe event identifier
 * @param type Stripe event type
 * @param rawPayload original verified event payload
 */
public record StripeWebhookEvent(
        String id,
        String type,
        String rawPayload
) {
}
