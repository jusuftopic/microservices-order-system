package com.example.investigationservice.service.explanation.deterministic;

import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationEvidence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicExplanationGeneratorTest {

    private final DeterministicExplanationGenerator generator =
            new DeterministicExplanationGenerator();

    @Test
    void returnsEmptyExplanationWithoutEvidence() {
        assertTrue(generator.generate(InvestigationContext.empty(42L)).isEmpty());
    }

    @Test
    void explainsKnownReasonAndRequestedDecision() {
        InvestigationEvidence evidence = evidence(
                "CREATED",
                "INVENTORY_RESERVE_COMPLETED",
                "INVENTORY_RESERVED",
                "PROCESS_PAYMENT",
                "PAYMENT_SERVICE",
                false,
                null
        );

        String explanation = generator.generate(context(evidence)).orElseThrow();

        assertEquals(
                "Inventory was reserved successfully for the order. "
                        + "Payment processing has been requested.",
                explanation
        );
    }

    @Test
    void explainsCompensationWithoutPresentingItAsCompleted() {
        InvestigationEvidence evidence = evidence(
                "INVENTORY_RESERVE_COMPLETED",
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "RELEASE_INVENTORY",
                "INVENTORY_SERVICE",
                true,
                "INVENTORY_RELEASE"
        );

        String explanation = generator.generate(context(evidence)).orElseThrow();

        assertEquals(
                "Payment processing failed. "
                        + "Releasing the reserved inventory has been requested. "
                        + "The inventory release is a compensating action.",
                explanation
        );
    }

    @Test
    void explainsOnlyLatestOrderedEvidence() {
        InvestigationEvidence inventoryReserved = evidence(
                "CREATED",
                "INVENTORY_RESERVE_COMPLETED",
                "INVENTORY_RESERVED",
                "PROCESS_PAYMENT",
                "PAYMENT_SERVICE",
                false,
                null
        );
        InvestigationEvidence paymentCompleted = evidence(
                "INVENTORY_RESERVE_COMPLETED",
                "PAYMENT_COMPLETED",
                "PAYMENT_COMPLETED",
                "COMMIT_INVENTORY",
                "INVENTORY_SERVICE",
                false,
                null
        );
        InvestigationContext context = new InvestigationContext(
                42L,
                "PAYMENT_COMPLETED",
                List.of(inventoryReserved, paymentCompleted)
        );

        String explanation = generator.generate(context).orElseThrow();

        assertEquals(
                "Payment was completed successfully. "
                        + "Committing the reserved inventory has been requested.",
                explanation
        );
    }

    @Test
    void fallsBackSafelyForUnknownContractValues() {
        InvestigationEvidence evidence = evidence(
                "PAYMENT_COMPLETED",
                "REVIEW_REQUIRED",
                "MANUAL_REVIEW_REQUIRED",
                "REQUEST_REVIEW",
                "REVIEW_SERVICE",
                true,
                "MANUAL_COMPENSATION"
        );

        String explanation = generator.generate(context(evidence)).orElseThrow();

        assertEquals(
                "The order moved from Payment Completed to Review Required. "
                        + "A follow-up action has been requested from Review Service. "
                        + "A compensating action is required.",
                explanation
        );
    }

    private InvestigationContext context(InvestigationEvidence evidence) {
        return new InvestigationContext(42L, evidence.newStatus(), List.of(evidence));
    }

    private InvestigationEvidence evidence(
            String previousStatus,
            String newStatus,
            String reasonCode,
            String decisionCode,
            String decisionTargetService,
            boolean compensationRequired,
            String compensationType
    ) {
        return new InvestigationEvidence(
                "ORDER_SERVICE",
                "TEST_EVENT",
                previousStatus,
                newStatus,
                reasonCode,
                decisionCode,
                decisionTargetService,
                compensationRequired,
                compensationType,
                Instant.parse("2026-08-19T10:00:00Z"),
                1
        );
    }
}
