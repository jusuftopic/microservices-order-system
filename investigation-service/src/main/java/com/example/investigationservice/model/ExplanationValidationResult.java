package com.example.investigationservice.model;

import java.util.Objects;

/**
 * Deterministic result of validating a generated explanation candidate.
 *
 * @param valid whether the candidate can be exposed
 * @param failureReason bounded reason for rejection, or {@link FailureReason#NONE}
 */
public record ExplanationValidationResult(
        boolean valid,
        FailureReason failureReason
) {

    public ExplanationValidationResult {
        Objects.requireNonNull(failureReason, "failureReason must not be null");
        if (valid != (failureReason == FailureReason.NONE)) {
            throw new IllegalArgumentException(
                    "Valid results require NONE; rejected results require a failure reason"
            );
        }
    }

    public static ExplanationValidationResult accepted() {
        return new ExplanationValidationResult(true, FailureReason.NONE);
    }

    public static ExplanationValidationResult rejected(FailureReason reason) {
        if (reason == FailureReason.NONE) {
            throw new IllegalArgumentException("A rejected response requires a failure reason");
        }
        return new ExplanationValidationResult(false, reason);
    }

    /**
     * Stable validation outcomes suitable for logs and low-cardinality metrics.
     */
    public enum FailureReason {
        NONE,
        MISSING_CANDIDATE,
        MISSING_CONTEXT,
        MISSING_EVIDENCE,
        MISSING_EXPLANATION,
        EXPLANATION_TOO_LONG,
        INVALID_CONTROL_CHARACTER,
        UNSUPPORTED_STATUS,
        STATUS_MISMATCH,
        UNSUPPORTED_REASON_CODE,
        REASON_CODE_MISMATCH,
        UNSUPPORTED_DECISION_CODE,
        DECISION_CODE_MISMATCH,
        UNSUPPORTED_COMPENSATION_TYPE,
        COMPENSATION_TYPE_MISMATCH
    }
}
