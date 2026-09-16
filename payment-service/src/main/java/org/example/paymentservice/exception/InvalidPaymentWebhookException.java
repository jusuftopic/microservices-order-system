package org.example.paymentservice.exception;

/**
 * Indicates that an incoming payment-provider webhook cannot be authenticated.
 */
public class InvalidPaymentWebhookException extends RuntimeException {

    public InvalidPaymentWebhookException(String message) {
        super(message);
    }

    public InvalidPaymentWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
