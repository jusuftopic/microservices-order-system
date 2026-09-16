package org.example.paymentservice.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.enums.PaymentProviderStatus;
import org.example.paymentservice.service.provider.clients.PaymentClient;
import org.example.paymentservice.exception.PaymentProviderNonRetryableException;
import org.example.paymentservice.exception.PaymentProviderRetryableException;
import org.springframework.stereotype.Service;

/**
 * Payment provider wrapper wraps client call with resiliency measures
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentProviderWrapper {

    private final PaymentClient paymentClient;

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "payment",
            fallbackMethod = "fallback"
    )
    @io.github.resilience4j.retry.annotation.Retry(name = "payment")
    public PaymentResultDTO pay(PaymentRequest request) {
       return paymentClient.pay(request);
    }

    /**
     * Fallback is triggered when:
     * - circuit breaker is OPEN
     * - retries exhausted
     * - timeout occurs
     */
    private PaymentResultDTO fallback(PaymentRequest request, Throwable ex) {
        if (ex instanceof PaymentProviderNonRetryableException exception) {
            log.error(
                    "[PAYMENT-SERVICE][PAYMENT-PROVIDER-WRAPPER] Non-retryable provider rejection. "
                            + "orderId={} idempotencyKey={} provider={} providerRequestId={} errorCode={}",
                    request.orderId(),
                    request.idempotencyKey(),
                    exception.getProvider(),
                    exception.getProviderRequestId(),
                    exception.getErrorCode(),
                    exception
            );
            return new PaymentResultDTO(
                    PaymentProviderStatus.FAILED,
                    null,
                    "PAYMENT_PROVIDER_REQUEST_REJECTED",
                    null,
                    exception.getProvider()
            );
        }

        if (ex instanceof
                io.github.resilience4j.circuitbreaker.CallNotPermittedException) {

            log.warn("[PAYMENT-SERVICE][PAYMENT-PROVIDER-WRAPPER] request rejected because circuit breaker " +
                            "is open. orderId={} idempotencyKey={}. Sending fallback response.",
                    request.orderId(), request.idempotencyKey());
        } else if (ex instanceof PaymentProviderRetryableException exception) {
            log.error(
                    "[PAYMENT-SERVICE][PAYMENT-PROVIDER-WRAPPER] Retryable provider failure exhausted. "
                            + "orderId={} idempotencyKey={} provider={} providerRequestId={} errorCode={}",
                    request.orderId(),
                    request.idempotencyKey(),
                    exception.getProvider(),
                    exception.getProviderRequestId(),
                    exception.getErrorCode(),
                    exception
            );
        } else {
            log.error("[PAYMENT-SERVICE][PAYMENT-PROVIDER-WRAPPER]  provider call failed after resilience " +
                            "handling. orderId={} idempotencyKey={} exceptionType={}. Sending fallback response.",
                    request.orderId(), request.idempotencyKey(), ex.getClass().getSimpleName(), ex);
        }

        return new PaymentResultDTO(
                PaymentProviderStatus.PROCESSING,
                null,
                "PAYMENT_PROVIDER_OUTCOME_UNKNOWN",
                null,
                "UNKNOWN"
        );
    }
}
