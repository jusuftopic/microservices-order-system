package com.example.investigationservice.service.explanation.ai;

/**
 * Signals that the external model did not complete within its request budget.
 */
public class ModelCallTimeoutException extends RuntimeException {

    public ModelCallTimeoutException(Throwable cause) {
        super("Model call exceeded its timeout", cause);
    }
}
