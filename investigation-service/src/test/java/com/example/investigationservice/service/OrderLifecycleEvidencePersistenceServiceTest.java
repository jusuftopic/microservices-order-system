package com.example.investigationservice.service;

import com.example.investigationservice.entity.OrderLifecycleEvidence;
import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.repository.OrderLifecycleEvidenceRepository;
import com.example.investigationservice.validation.OrderLifecycleEventValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderLifecycleEvidencePersistenceServiceTest {

    @Test
    void recordsMetricWhenConcurrentInsertIsDetected() {
        UUID messageId = UUID.randomUUID();
        OrderLifecycleEvidenceRepository repository =
                mock(OrderLifecycleEvidenceRepository.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        InvestigationMetrics metrics = new InvestigationMetrics(registry);
        OrderLifecycleEvidencePersistenceService service =
                new OrderLifecycleEvidencePersistenceService(
                        repository,
                        new OrderLifecycleEventValidator(),
                        metrics
                );

        when(repository.existsByMessageId(messageId)).thenReturn(false, true);
        when(repository.saveAndFlush(any(OrderLifecycleEvidence.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate message ID"));

        boolean persisted = service.persist(
                lifecycleEvent(messageId),
                "order.lifecycle.v1",
                0,
                7L
        );

        assertThat(persisted).isFalse();
        assertThat(registry.get("investigation.lifecycle.concurrent.inserts.total")
                .counter()
                .count())
                .isEqualTo(1.0);
    }

    private OrderLifecycleTransitionedEvent lifecycleEvent(UUID messageId) {
        return new OrderLifecycleTransitionedEvent(
                42L,
                "PAYMENT_COMPLETED",
                "PROCESSING",
                "PAYMENT_SUCCEEDED",
                "ORDER_SERVICE",
                "PAYMENT_COMPLETED",
                UUID.randomUUID(),
                null,
                OrderLifecycleTransitionedEvent.Compensation.notRequired(),
                Instant.parse("2026-08-16T10:15:30Z"),
                1,
                "correlation-42",
                messageId
        );
    }
}
