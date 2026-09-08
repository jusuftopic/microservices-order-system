package org.example.paymentservice.service.provider.clients;

/**
 * Represents a failed interaction with the Stripe payment provider.
 */
public class StripePaymentProviderException extends RuntimeException {

    public StripePaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
