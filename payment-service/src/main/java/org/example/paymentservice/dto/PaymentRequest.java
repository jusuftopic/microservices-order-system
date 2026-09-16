package org.example.paymentservice.dto;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * Provider-neutral information required to initiate one payment.
 *
 * @param paymentId internal payment identifier
 * @param orderId order being paid
 * @param amount amount expressed in the currency's major unit
 * @param currency three-letter ISO currency code
 * @param correlationId distributed workflow correlation identifier
 * @param idempotencyKey stable identifier for this payment operation
 */
public record PaymentRequest(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        String currency,
        String correlationId,
        String idempotencyKey
) {

    public PaymentRequest {
        if (Objects.requireNonNull(paymentId, "paymentId must not be null") <= 0) {
            throw new IllegalArgumentException("paymentId must be greater than zero");
        }
        if (Objects.requireNonNull(orderId, "orderId must not be null") <= 0) {
            throw new IllegalArgumentException("orderId must be greater than zero");
        }
        if (Objects.requireNonNull(amount, "amount must not be null").signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        currency = requireText(currency, "currency").toUpperCase(Locale.ROOT);
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be a three-letter ISO code");
        }
        correlationId = requireText(correlationId, "correlationId");
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
