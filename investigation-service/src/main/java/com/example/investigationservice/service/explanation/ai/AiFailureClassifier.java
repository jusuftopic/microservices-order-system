package com.example.investigationservice.service.explanation.ai;

import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * Classifies model failures into bounded operational categories.
 */
@Component
public class AiFailureClassifier {

    /**
     * Classifies a model failure for retry and circuit-breaker decisions.
     *
     * @param exception model failure
     * @return operational failure category
     */
    public FailureType classify(Throwable exception) {
        if (hasCause(exception, NonTransientAiException.class)) {
            return FailureType.NON_TRANSIENT;
        }
        ClientException clientException = findCause(exception, ClientException.class);
        if (clientException != null) {
            return clientException.code() == 408 || clientException.code() == 429
                    ? FailureType.TRANSIENT : FailureType.NON_TRANSIENT;
        }
        if (isTimeout(exception)
                || hasCause(exception, TransientAiException.class)
                || hasCause(exception, ServerException.class)
                || hasCause(exception, IOException.class)) {
            return FailureType.TRANSIENT;
        }
        return FailureType.UNCLASSIFIED;
    }

    /**
     * Determines whether an exception chain represents a timeout.
     *
     * @param exception model failure
     * @return {@code true} when a timeout cause is present
     */
    public boolean isTimeout(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException
                    || cause instanceof TimeoutException
                    || (cause instanceof InterruptedIOException
                    && cause.getMessage() != null
                    && cause.getMessage().toLowerCase().contains("timeout"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private <T extends Throwable> boolean hasCause(
            Throwable exception,
            Class<T> expectedType
    ) {
        return findCause(exception, expectedType) != null;
    }

    private <T extends Throwable> T findCause(
            Throwable exception,
            Class<T> expectedType
    ) {
        Throwable cause = exception;
        while (cause != null) {
            if (expectedType.isInstance(cause)) {
                return expectedType.cast(cause);
            }
            cause = cause.getCause();
        }
        return null;
    }

    public enum FailureType {
        TRANSIENT("transient"),
        NON_TRANSIENT("non_transient"),
        UNCLASSIFIED("unclassified");

        private final String metricValue;

        FailureType(String metricValue) {
            this.metricValue = metricValue;
        }

        public String metricValue() {
            return metricValue;
        }
    }
}
