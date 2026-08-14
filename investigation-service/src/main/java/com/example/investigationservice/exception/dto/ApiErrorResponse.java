package com.example.investigationservice.exception.dto;

import java.time.Instant;
import java.util.List;

/**
 * Stable error contract returned by the Investigation API.
 *
 * @param timestamp time at which the error response was created
 * @param status    HTTP status code
 * @param error     HTTP error category
 * @param message   safe human-readable description
 * @param path      request path that produced the error
 * @param violations individual input violations, when applicable
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<Violation> violations
) {

    public ApiErrorResponse {
        violations = List.copyOf(violations);
    }

    /**
     * Describes a rejected request value.
     *
     * @param field field or parameter name
     * @param message validation message
     */
    public record Violation(String field, String message) {
    }
}
