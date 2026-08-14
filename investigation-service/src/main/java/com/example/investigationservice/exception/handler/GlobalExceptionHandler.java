package com.example.investigationservice.exception.handler;

import com.example.investigationservice.exception.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Converts Investigation API failures into a consistent and sanitized error contract.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation failures on controller parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.Violation> violations = exception.getConstraintViolations()
                .stream()
                .map(this::toViolation)
                .toList();

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request.getRequestURI(),
                violations
        );
    }

    /**
     * Handles values that cannot be converted to the declared controller type.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse.Violation violation = new ApiErrorResponse.Violation(
                exception.getName(),
                exception.getName() + " must be a positive whole number"
        );

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request.getRequestURI(),
                List.of(violation)
        );
    }

    /**
     * Handles invalid arguments rejected below the HTTP validation boundary.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        );
    }

    /**
     * Preserves the correct status when an endpoint does not support the HTTP method.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpServletRequest request) {
        return errorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method is not supported for this endpoint",
                request.getRequestURI(),
                List.of()
        );
    }

    /**
     * Returns the standard error contract when no API or static resource matches the path.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(HttpServletRequest request) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "Requested endpoint was not found",
                request.getRequestURI(),
                List.of()
        );
    }

    /**
     * Prevents internal exception details from leaking to API clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unexpected Investigation API failure", exception);

        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error occurred",
                request.getRequestURI(),
                List.of()
        );
    }

    private ApiErrorResponse.Violation toViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();
        int separator = field.lastIndexOf('.');

        if (separator >= 0) {
            field = field.substring(separator + 1);
        }

        return new ApiErrorResponse.Violation(field, violation.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> errorResponse(
            HttpStatus status,
            String message,
            String path,
            List<ApiErrorResponse.Violation> violations
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                violations
        );

        return ResponseEntity.status(status).body(response);
    }
}
