package com.example.investigationservice.entity;

import com.example.investigationservice.repository.OrderLifecycleEvidenceRepository;
import com.example.investigationservice.service.OrderLifecycleEvidencePersistenceService;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

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
@Import(OrderLifecycleEvidencePersistenceService.class)
class OrderLifecycleEvidencePersistenceTest {

    @Autowired
    private OrderLifecycleEvidencePersistenceService persistenceService;

    @Autowired
    private OrderLifecycleEvidenceRepository repository;

    @Test
    void mapsAndPersistsIncomingLifecycleEvidenceIdempotently() {
        UUID messageId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        OrderLifecycleTransitionedEvent event = new OrderLifecycleTransitionedEvent(
                42L,
                "INVENTORY_RESERVE_COMPLETED",
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "PAYMENT_SERVICE",
                "PAYMENT_FAILED",
                causationId,
                new OrderLifecycleTransitionedEvent.OrchestrationDecision(
                        "RELEASE_INVENTORY",
                        "INVENTORY_SERVICE",
                        commandId
                ),
                new OrderLifecycleTransitionedEvent.Compensation(
                        true,
                        "INVENTORY_RELEASE"
                ),
                Instant.parse("2026-08-15T10:15:30Z"),
                1,
                "correlation-42",
                messageId
        );

        assertThat(persistenceService.persist(event, "order.lifecycle.v1", 0, 7L))
                .isTrue();
        assertThat(persistenceService.persist(event, "order.lifecycle.v1", 0, 7L))
                .isFalse();

        assertThat(repository.findAll()).singleElement().satisfies(stored -> {
            assertThat(stored.getMessageId()).isEqualTo(messageId);
            assertThat(stored.getOrderId()).isEqualTo(42L);
            assertThat(stored.getPreviousStatus()).isEqualTo("INVENTORY_RESERVE_COMPLETED");
            assertThat(stored.getNewStatus()).isEqualTo("PAYMENT_FAILED");
            assertThat(stored.getCausationId()).isEqualTo(causationId);
            assertThat(stored.getDecisionCommandId()).isEqualTo(commandId);
            assertThat(stored.isCompensationRequired()).isTrue();
            assertThat(stored.getKafkaTopic()).isEqualTo("order.lifecycle.v1");
            assertThat(stored.getKafkaPartition()).isZero();
            assertThat(stored.getKafkaOffset()).isEqualTo(7L);
            assertThat(stored.getReceivedAt()).isNotNull();
        });
    }
}
