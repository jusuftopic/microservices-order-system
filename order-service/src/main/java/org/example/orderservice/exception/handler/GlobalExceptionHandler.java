package org.example.orderservice.exception.handler;


import org.example.orderservice.exception.dto.ApiErrorResponse;
import org.example.orderservice.exception.types.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Global exception handler for the REST API.
 *
 * <p>This class centralizes exception handling so that controllers
 * do not need to implement repetitive try-catch logic.</p>
 *
 * <p>It ensures that all errors returned by the API follow a consistent
 * response structure ({@link ApiErrorResponse}).</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles cases where a requested resource cannot be found.
     *
     * @param ex thrown NotFoundException
     * @return standardized error response with HTTP 404
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiErrorResponse.of(ex.getMessage(), 404)
                );
    }

    /**
     * Handles cases where an input validation error occurs.
     *
     * @param ex thrown MethodArgumentNotValidException
     * @return standardized error response with HTTP 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {

        List<ApiErrorResponse.FieldError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ApiErrorResponse.FieldError(
                        err.getField(),
                        err.getDefaultMessage()
                ))
                .toList();

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("Validation failed", 400, errors));
    }

    /**
     * Handles cases where malformatted JSON error occurs.
     *
     * @param ex thrown HttpMessageNotReadableException
     * @return standardized error response with HTTP 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<org.example.orderservice.exception.dto.ApiErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("Malformed JSON request", 400));
    }

    /**
     * Handles illegal state exceptions.
     *
     * <p>This prevents leaking stack traces or internal details to the client.</p>
     *
     * @param ex thrown IllegalStateException
     * @return standardized error response with HTTP 400
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex)
    {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("Illegal state error occurred", 400));
    }

    /**
     * Handles all unhandled exceptions as a fallback.
     *
     * <p>This prevents leaking stack traces or internal details to the client.</p>
     *
     * @param ex unexpected exception
     * @return standardized error response with HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiErrorResponse.of("Unexpected error occurred", 500)
                );
    }
}
