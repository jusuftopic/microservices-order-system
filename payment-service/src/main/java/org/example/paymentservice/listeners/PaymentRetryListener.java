package org.example.paymentservice.listeners;

import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Observability component for the "payment" Retry mechanism.
 *
 * <p>Purpose:
 * - Logs each retry attempt
 * - Captures successful retries
 * - Detects when retry attempts are exhausted</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRetryListener {

    private final RetryRegistry retryRegistry;

    /**
     * Registers retry event listeners after application startup.
     *
     * <p>Events captured:
     * - Each retry attempt with attempt count
     * - Successful recovery after retries
     * - Failure after max retry attempts reached</p>
     */
    @PostConstruct
    public void register() {
        retryRegistry
                .retry("payment")
                .getEventPublisher()

                /**
                 * Triggered before each retry attempt.
                 * Used for tracking retry count and exception type.
                 */
                .onRetry(event ->
                    log.warn("[PAYMENT-PROVIDER][RETRY] Attempt {} failed due to {}",
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable() != null
                            ? event.getLastThrowable().getClass().getSimpleName()
                            : null)
                )

                /**
                 * Triggered when a retry eventually succeeds.
                 */
                .onSuccess(event ->
                        log.info("[PAYMENT-PROVIDER][RETRY] Success after {} attempts",
                                event.getNumberOfRetryAttempts())
                )

                /**
                 * Triggered when all retry attempts are exhausted.
                 */
                .onError(event ->
                        log.error("[PAYMENT-PROVIDER][RETRY] Exhausted retries for operation '{}'",
                                event.getName())
                );
    }
}
