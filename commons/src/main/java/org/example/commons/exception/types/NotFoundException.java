package org.example.commons.exception.types;

/**
 * Custom exception for not founded resources
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
