package org.example.messagingstarter.contracts.lifecycle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleVocabularyTest {

    @Test
    void resolvesKnownReasonWithoutRejectingUnknownReason() {
        assertEquals(
                LifecycleReasonCode.PAYMENT_COMPLETED,
                LifecycleReasonCode.fromCode("PAYMENT_COMPLETED").orElseThrow()
        );
        assertTrue(LifecycleReasonCode.fromCode("NEW_REASON_FROM_NEWER_PRODUCER").isEmpty());
    }

    @Test
    void derivesDecisionTargetFromSharedVocabulary() {
        assertEquals(
                ServiceName.NOTIFICATION_SERVICE,
                OrchestrationDecisionCode.SEND_NOTIFICATION.targetService()
        );
    }

    @Test
    void derivesTriggerSourceFromSharedVocabulary() {
        assertEquals(
                ServiceName.INVENTORY_SERVICE,
                LifecycleTrigger.INVENTORY_COMMIT_COMPLETED.sourceService()
        );
    }
}
