package com.example.investigationservice.exception;

/**
 * Indicates that an ownership event cannot safely update the local projection.
 */
public class InvalidOrderOwnershipEventException extends RuntimeException {

    /**
     * Creates an ownership contract failure.
     *
     * @param message diagnostic reason that excludes sensitive payload data
     */
    public InvalidOrderOwnershipEventException(String message) {
        super(message);
    }
}
