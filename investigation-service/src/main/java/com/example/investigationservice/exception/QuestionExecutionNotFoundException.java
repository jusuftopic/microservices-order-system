package com.example.investigationservice.exception;

import java.util.UUID;

/**
 * Indicates that feedback referenced an unknown question execution.
 */
public class QuestionExecutionNotFoundException extends RuntimeException {

    public QuestionExecutionNotFoundException(UUID executionId) {
        super(String.format("Investigation question execution %s was not found", executionId));
    }
}
