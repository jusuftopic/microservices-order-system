package com.example.investigationservice.service.explanation;

import com.example.investigationservice.model.AiExplanationResponse;
import com.example.investigationservice.model.ExplanationValidationResult;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationEvidence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.example.investigationservice.model.ExplanationValidationResult.FailureReason;
import static com.example.investigationservice.service.explanation.ai.prompt.InvestigationPromptFactory.MAX_EXPLANATION_CHARACTERS;
import static org.assertj.core.api.Assertions.assertThat;

class ExplanationValidationServiceTest {

    private final ExplanationValidationService validationService =
            new ExplanationValidationService();

    @Test
    void acceptsCandidateGroundedInLatestEvidence() {
        ExplanationValidationResult result = validationService.validate(
                candidate("Explanation", "PAYMENT_FAILED", "PAYMENT_FAILED",
                        "RELEASE_INVENTORY", "INVENTORY_RELEASE"),
                context(evidence("PAYMENT_FAILED", "PAYMENT_FAILED",
                        "RELEASE_INVENTORY", "INVENTORY_RELEASE"))
        );

        assertThat(result.valid()).isTrue();
        assertThat(result.failureReason()).isEqualTo(FailureReason.NONE);
    }

    @Test
    void rejectsInvalidResponseContractAndEvidenceMappings() {
        InvestigationContext validContext = context(evidence(
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "RELEASE_INVENTORY",
                "INVENTORY_RELEASE"
        ));
        AiExplanationResponse validCandidate = candidate(
                "Explanation",
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "RELEASE_INVENTORY",
                "INVENTORY_RELEASE"
        );

        assertRejected(null, validContext, FailureReason.MISSING_CANDIDATE);
        assertRejected(validCandidate, null, FailureReason.MISSING_CONTEXT);
        assertRejected(validCandidate, InvestigationContext.empty(42L),
                FailureReason.MISSING_EVIDENCE);
        assertRejected(candidate("  ", "PAYMENT_FAILED", "PAYMENT_FAILED",
                        "RELEASE_INVENTORY", "INVENTORY_RELEASE"),
                validContext, FailureReason.MISSING_EXPLANATION);
        assertRejected(candidate("X".repeat(MAX_EXPLANATION_CHARACTERS + 1),
                        "PAYMENT_FAILED", "PAYMENT_FAILED", "RELEASE_INVENTORY",
                        "INVENTORY_RELEASE"),
                validContext, FailureReason.EXPLANATION_TOO_LONG);
        assertRejected(candidate("Invalid\u0000explanation", "PAYMENT_FAILED",
                        "PAYMENT_FAILED", "RELEASE_INVENTORY", "INVENTORY_RELEASE"),
                validContext, FailureReason.INVALID_CONTROL_CHARACTER);
        assertRejected(candidate("Explanation", "UNKNOWN_STATUS", "PAYMENT_FAILED",
                        "RELEASE_INVENTORY", "INVENTORY_RELEASE"),
                validContext, FailureReason.UNSUPPORTED_STATUS);
        assertRejected(candidate("Explanation", "PAYMENT_COMPLETED", "PAYMENT_FAILED",
                        "RELEASE_INVENTORY", "INVENTORY_RELEASE"),
                validContext, FailureReason.STATUS_MISMATCH);
        assertRejected(candidate("Explanation", "PAYMENT_FAILED", "OTHER_REASON",
                        "RELEASE_INVENTORY", "INVENTORY_RELEASE"),
                validContext, FailureReason.REASON_CODE_MISMATCH);
        assertRejected(candidate("Explanation", "PAYMENT_FAILED", "PAYMENT_FAILED",
                        "COMMIT_INVENTORY", "INVENTORY_RELEASE"),
                validContext, FailureReason.DECISION_CODE_MISMATCH);
        assertRejected(candidate("Explanation", "PAYMENT_FAILED", "PAYMENT_FAILED",
                        "RELEASE_INVENTORY", "PAYMENT_REFUND"),
                validContext, FailureReason.COMPENSATION_TYPE_MISMATCH);
    }

    @Test
    void rejectsUnknownCodesEvenWhenTheyMatchEvidence() {
        assertRejected(
                candidate("Explanation", "PAYMENT_FAILED", "UNKNOWN_REASON", null, null),
                context(evidence("PAYMENT_FAILED", "UNKNOWN_REASON", null, null)),
                FailureReason.UNSUPPORTED_REASON_CODE
        );
        assertRejected(
                candidate("Explanation", "PAYMENT_FAILED", "PAYMENT_FAILED",
                        "UNKNOWN_DECISION", null),
                context(evidence("PAYMENT_FAILED", "PAYMENT_FAILED",
                        "UNKNOWN_DECISION", null)),
                FailureReason.UNSUPPORTED_DECISION_CODE
        );
        assertRejected(
                candidate("Explanation", "PAYMENT_FAILED", "PAYMENT_FAILED", null,
                        "UNKNOWN_COMPENSATION"),
                context(evidence("PAYMENT_FAILED", "PAYMENT_FAILED", null,
                        "UNKNOWN_COMPENSATION")),
                FailureReason.UNSUPPORTED_COMPENSATION_TYPE
        );
    }

    @Test
    void acceptsOptionalCodesOnlyWhenBothCandidateAndEvidenceContainNull() {
        ExplanationValidationResult result = validationService.validate(
                candidate("Inventory reservation failed.",
                        "INVENTORY_RESERVE_FAILED", "INVENTORY_RESERVATION_FAILED",
                        null, null),
                context(evidence("INVENTORY_RESERVE_FAILED",
                        "INVENTORY_RESERVATION_FAILED", null, null))
        );

        assertThat(result.valid()).isTrue();
    }

    private void assertRejected(
            AiExplanationResponse candidate,
            InvestigationContext context,
            FailureReason expectedReason
    ) {
        ExplanationValidationResult result = validationService.validate(candidate, context);

        assertThat(result.valid()).isFalse();
        assertThat(result.failureReason()).isEqualTo(expectedReason);
    }

    private InvestigationContext context(InvestigationEvidence evidence) {
        return new InvestigationContext(42L, evidence.newStatus(), List.of(evidence));
    }

    private InvestigationEvidence evidence(
            String newStatus,
            String reasonCode,
            String decisionCode,
            String compensationType
    ) {
        return new InvestigationEvidence(
                "ORDER_SERVICE",
                "EVENT_ORDER_LIFECYCLE_TRANSITIONED",
                "INVENTORY_RESERVE_COMPLETED",
                newStatus,
                reasonCode,
                decisionCode,
                decisionCode == null ? null : "INVENTORY_SERVICE",
                compensationType != null,
                compensationType,
                Instant.parse("2026-08-29T08:00:00Z"),
                1
        );
    }

    private AiExplanationResponse candidate(
            String explanation,
            String currentStatus,
            String reasonCode,
            String decisionCode,
            String compensationType
    ) {
        return new AiExplanationResponse(
                explanation,
                currentStatus,
                reasonCode,
                decisionCode,
                compensationType
        );
    }
}
