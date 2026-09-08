package com.example.investigationservice.service.explanation.ai;

import com.example.investigationservice.exception.ModelCircuitOpenException;
import com.example.investigationservice.metrics.InvestigationMetrics;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Protects model communication from sustained provider degradation.
 */
@Component
@ConditionalOnProperty(name = "app.ai.generator", havingValue = "model")
@Slf4j
public class AiCircuitBreakerExecutor {

    private final CircuitBreaker circuitBreaker;

    /**
     * Creates the provider circuit and registers state-transition reporting.
     */
    public AiCircuitBreakerExecutor(
            InvestigationMetrics metrics,
            AiFailureClassifier failureClassifier,
            @Value("${app.ai.provider}") String provider,
            @Value("${app.ai.model}") String model,
            @Value("${app.ai.circuit-breaker.sliding-window-size}") int slidingWindowSize,
            @Value("${app.ai.circuit-breaker.minimum-calls}") int minimumCalls,
            @Value("${app.ai.circuit-breaker.failure-rate-threshold}") float failureRateThreshold,
            @Value("${app.ai.circuit-breaker.open-duration}") Duration openDuration,
            @Value("${app.ai.circuit-breaker.half-open-calls}") int halfOpenCalls
    ) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumCalls)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(openDuration)
                .permittedNumberOfCallsInHalfOpenState(halfOpenCalls)
                .recordException(exception -> failureClassifier.classify(exception)
                        == AiFailureClassifier.FailureType.TRANSIENT)
                .ignoreException(exception -> failureClassifier.classify(exception)
                        != AiFailureClassifier.FailureType.TRANSIENT)
                .build();
        this.circuitBreaker = CircuitBreaker.of("ai-explanation", config);
        this.circuitBreaker.getEventPublisher().onStateTransition(event -> {
            CircuitBreaker.StateTransition transition = event.getStateTransition();
            String fromState = transition.getFromState().name().toLowerCase();
            String toState = transition.getToState().name().toLowerCase();
            metrics.recordAiCircuitTransition(fromState, toState, provider, model);
            if (transition.getToState() == CircuitBreaker.State.OPEN) {
                log.warn(
                        "[INVESTIGATION-SERVICE][AI-CIRCUIT] State changed from {} to {} "
                                + "for provider {} and model {}",
                        transition.getFromState(), transition.getToState(), provider, model
                );
            } else {
                log.info(
                        "[INVESTIGATION-SERVICE][AI-CIRCUIT] State changed from {} to {} "
                                + "for provider {} and model {}",
                        transition.getFromState(), transition.getToState(), provider, model
                );
            }
        });
    }

    /**
     * Executes one complete, retry-aware model operation through the circuit.
     *
     * @param operation operation to execute
     * @param <T> operation result type
     * @return result returned by the operation
     */
    public <T> T execute(Supplier<T> operation) {
        try {
            return circuitBreaker.executeSupplier(operation);
        } catch (CallNotPermittedException exception) {
            throw new ModelCircuitOpenException(exception);
        }
    }
}
