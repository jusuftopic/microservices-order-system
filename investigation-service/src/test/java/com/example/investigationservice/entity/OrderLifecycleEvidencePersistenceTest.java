package com.example.investigationservice.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:evidence-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OrderLifecycleEvidencePersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsLifecycleEvidenceAndInitializesReceiptTime() {
        OrderLifecycleEvidence evidence = OrderLifecycleEvidence.builder()
                .messageId(UUID.randomUUID())
                .orderId(42L)
                .previousStatus("INVENTORY_RESERVE_COMPLETED")
                .newStatus("PAYMENT_FAILED")
                .reasonCode("PAYMENT_FAILED")
                .sourceService("PAYMENT_SERVICE")
                .sourceEventType("PAYMENT_FAILED")
                .causationId(UUID.randomUUID())
                .decisionCode("RELEASE_INVENTORY")
                .decisionTargetService("INVENTORY_SERVICE")
                .decisionCommandId(UUID.randomUUID())
                .compensationRequired(true)
                .compensationType("INVENTORY_RELEASE")
                .occurredAt(Instant.parse("2026-08-15T10:15:30Z"))
                .eventVersion(1)
                .correlationId("correlation-42")
                .kafkaTopic("order.lifecycle.v1")
                .kafkaPartition(0)
                .kafkaOffset(7L)
                .build();

        OrderLifecycleEvidence stored = entityManager.persistFlushFind(evidence);

        assertThat(stored.getId()).isNotNull();
        assertThat(stored.getMessageId()).isEqualTo(evidence.getMessageId());
        assertThat(stored.getNewStatus()).isEqualTo("PAYMENT_FAILED");
        assertThat(stored.getReceivedAt()).isNotNull();
    }
}
