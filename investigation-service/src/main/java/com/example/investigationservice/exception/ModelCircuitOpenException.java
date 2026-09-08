package com.example.investigationservice.exception;

/**
 * Indicates that model communication was skipped because its circuit is open.
 */
public class ModelCircuitOpenException extends RuntimeException {

    public ModelCircuitOpenException(Throwable cause) {
        super("Model circuit is open", cause);
    }
}
