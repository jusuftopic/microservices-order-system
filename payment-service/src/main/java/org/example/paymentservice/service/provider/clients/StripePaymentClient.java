package org.example.paymentservice.service.provider.clients;

import com.stripe.StripeClient;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.ApiException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.dto.RefundRequest;
import org.example.paymentservice.dto.RefundResult;
import org.example.paymentservice.exception.PaymentProviderNonRetryableException;
import org.example.paymentservice.exception.PaymentProviderRetryableException;
import org.example.paymentservice.mapper.StripePaymentResultMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * Stripe implementation of the payment-provider boundary.
 */
@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
@RequiredArgsConstructor
@Slf4j
public class StripePaymentClient implements PaymentClient {

    private final StripeClient stripeClient;
    private final StripePaymentResultMapper resultMapper;

    @Value("${app.payment.stripe.test-payment-method}")
    private String paymentMethod;

    /**
     * Creates and confirms one PaymentIntent for an order. The command ID is
     * propagated as Stripe's idempotency key so a retried call cannot create a
     * second intent for the same operation.
     */
    @Override
    public PaymentResultDTO pay(PaymentRequest request) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(toMinorUnits(request))
                    .setCurrency(request.currency().toLowerCase(Locale.ROOT))
                    .setPaymentMethod(paymentMethod)
                    .setConfirm(true)
                    .addPaymentMethodType("card")
                    .putMetadata("payment_id", request.paymentId().toString())
                    .putMetadata("order_id", request.orderId().toString())
                    .putMetadata("correlation_id", request.correlationId())
                    .putMetadata("idempotency_key", request.idempotencyKey())
                    .build();
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(request.idempotencyKey())
                    .build();

            PaymentIntent intent = stripeClient.v1()
                    .paymentIntents()
                    .create(params, options);
            PaymentResultDTO result = resultMapper.toResult(intent);
            log.info(
                    "[PAYMENT-PROVIDER][STRIPE] PaymentIntent {} for order {} reached state {}",
                    intent.getId(),
                    request.orderId(),
                    result.status()
            );
            return result;
        } catch (StripeException exception) {
            throw translate(exception);
        } catch (Exception exception) {
            throw translateUnexpected(exception);
        }
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        log.warn(
                "[PAYMENT-PROVIDER][STRIPE] Refund integration is not implemented; "
                        + "refund operation {} was not sent to Stripe",
                request.refundOperationId()
        );
        return new RefundResult(false, null, "STRIPE_REFUND_NOT_IMPLEMENTED");
    }

    static RuntimeException translate(StripeException exception) {
        String message = "Stripe could not process the PaymentIntent request";
        if (isRetryable(exception)) {
            return new PaymentProviderRetryableException(
                    message,
                    "STRIPE",
                    exception.getRequestId(),
                    exception.getCode(),
                    exception
            );
        }
        return new PaymentProviderNonRetryableException(
                message,
                "STRIPE",
                exception.getRequestId(),
                exception.getCode(),
                exception
        );
    }

    private static boolean isRetryable(StripeException exception) {
        if (exception instanceof ApiConnectionException
                || exception instanceof RateLimitException) {
            return true;
        }
        if (exception instanceof ApiException && exception.getStatusCode() != null) {
            int status = exception.getStatusCode();
            return status == 408 || status == 424 || status >= 500;
        }
        return false;
    }

    static RuntimeException translateUnexpected(Exception exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof IOException || cause instanceof TimeoutException) {
                return new PaymentProviderRetryableException(
                        "Temporary failure while preparing or sending the Stripe request",
                        "STRIPE",
                        null,
                        null,
                        exception
                );
            }
            cause = cause.getCause();
        }
        return new PaymentProviderNonRetryableException(
                "Unexpected failure while preparing or sending the Stripe request",
                "STRIPE",
                null,
                null,
                exception
        );
    }

    private long toMinorUnits(PaymentRequest request) {
        Currency currency = Currency.getInstance(request.currency().toUpperCase(Locale.ROOT));
        int fractionDigits = currency.getDefaultFractionDigits();
        return request.amount()
                .setScale(fractionDigits, RoundingMode.UNNECESSARY)
                .movePointRight(fractionDigits)
                .longValueExact();
    }

}
