package com.example.investigationservice.exception;

/**
 * Signals that a lifecycle event violates the Investigation Service's
 * structural ingestion contract.
 */
public class InvalidLifecycleEventException extends IllegalArgumentException {

    /**
     * Creates an exception containing the rejected contract details.
     *
     * @param message validation failure description
     */
    public InvalidLifecycleEventException(String message) {
        super(message);
    }
}
