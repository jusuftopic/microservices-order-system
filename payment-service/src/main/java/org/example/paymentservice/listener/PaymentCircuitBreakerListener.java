package org.example.paymentservice.listener;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Observability component for the "payment" CircuitBreaker.
 *
 * <p>Purpose:
 * - Logs state transitions (CLOSED, OPEN, HALF_OPEN)
 * - Tracks rejected calls when circuit is OPEN
 * - Captures recorded failures for diagnostics</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCircuitBreakerListener {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Registers event listeners after Spring context initialization.
     *
     * <p>Events captured:
     * - State transitions (OPEN/CLOSE/HALF_OPEN)
     * - Calls rejected due to OPEN state
     * - Errors recorded by CircuitBreaker</p>
     */
    @PostConstruct
    public void register() {
        circuitBreakerRegistry
                .circuitBreaker("payment")
                .getEventPublisher()
                .onStateTransition(event ->
                        log.warn("[PAYMENT-PROVIDER][CB] Circuit breaker state transition from {} to {}",
                                event.getStateTransition().getFromState(), event.getStateTransition().getToState())
                )
                .onCallNotPermitted(event ->
                        log.warn("[PAYMENT-PROVIDER][CB] call not permitted (OPEN state)")
                )
                .onError(event ->
                        log.error("[PAYMENT-PROVIDER][CB] error recorded: {}", event.getThrowable().getMessage())
                );
    }
}
