package com.example.investigationservice.exception;

/**
 * Indicates that an execution already has feedback with different content.
 */
public class QuestionFeedbackConflictException extends RuntimeException {

    public QuestionFeedbackConflictException() {
        super("Feedback already submitted");
    }
}
