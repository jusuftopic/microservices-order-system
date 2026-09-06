package com.example.investigationservice.exception;

/**
 * Signals that the external model did not complete within its request budget.
 */
public class ModelCallTimeoutException extends RuntimeException {

    public ModelCallTimeoutException(Throwable cause) {
        super("Model call exceeded its timeout", cause);
    }
}
