package com.example.investigationservice.entity;

import com.example.investigationservice.exception.InvalidOrderOwnershipEventException;
import com.example.investigationservice.repository.OrderOwnerRepository;
import com.example.investigationservice.service.OrderOwnershipService;
import com.example.investigationservice.validation.OrderOwnershipEventValidator;
import org.example.messagingstarter.contracts.events.OrderOwnershipAssignedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies persistence, idempotency, and authorization lookup for ownership facts.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ownership-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({OrderOwnershipService.class, OrderOwnershipEventValidator.class})
class OrderOwnershipPersistenceTest {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID MESSAGE_ID =
            UUID.fromString("2b997737-7918-442b-bbd8-c26e52fce083");

    @Autowired
    private OrderOwnershipService ownershipService;

    @Autowired
    private OrderOwnerRepository repository;

    /**
     * Verifies idempotent storage and exact local ownership checks.
     */
    @Test
    void persistsOwnershipIdempotentlyAndChecksOwner() {
        OrderOwnershipAssignedEvent event = ownershipEvent(CUSTOMER_ID, MESSAGE_ID);

        assertThat(ownershipService.persist(event, "order.ownership.v1", 0, 7L))
                .isTrue();
        assertThat(ownershipService.persist(event, "order.ownership.v1", 0, 7L))
                .isFalse();

        assertThat(repository.findAll()).singleElement().satisfies(stored -> {
            assertThat(stored.getOrderId()).isEqualTo(42L);
            assertThat(stored.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(stored.getMessageId()).isEqualTo(MESSAGE_ID);
            assertThat(stored.getOccurredAt())
                    .isEqualTo(Instant.parse("2026-10-27T10:15:00Z"));
            assertThat(stored.getReceivedAt()).isNotNull();
        });

        assertThat(ownershipService.isOwner(42L, CUSTOMER_ID)).isTrue();
        assertThat(ownershipService.isOwner(42L, UUID.randomUUID())).isFalse();
        assertThat(ownershipService.isOwner(99L, CUSTOMER_ID)).isFalse();
        assertThat(ownershipService.isOwner(null, CUSTOMER_ID)).isFalse();
        assertThat(ownershipService.isOwner(42L, null)).isFalse();
    }

    /**
     * Verifies that ownership cannot be silently reassigned by another event.
     */
    @Test
    void rejectsConflictingOwnershipAssignment() {
        ownershipService.persist(
                ownershipEvent(CUSTOMER_ID, MESSAGE_ID),
                "order.ownership.v1",
                0,
                7L
        );

        OrderOwnershipAssignedEvent conflicting = ownershipEvent(
                UUID.fromString("33333333-3333-4333-8333-333333333333"),
                UUID.fromString("44444444-4444-4444-8444-444444444444")
        );

        assertThatThrownBy(() -> ownershipService.persist(
                conflicting,
                "order.ownership.v1",
                0,
                8L
        )).isInstanceOf(InvalidOrderOwnershipEventException.class);

        assertThat(ownershipService.isOwner(42L, CUSTOMER_ID)).isTrue();
        assertThat(repository.count()).isOne();
    }

    /**
     * Verifies that malformed events do not create authorization state.
     */
    @Test
    void rejectsInvalidOwnershipEventWithoutPersistence() {
        OrderOwnershipAssignedEvent invalid = new OrderOwnershipAssignedEvent(
                0L,
                CUSTOMER_ID,
                Instant.parse("2026-10-27T10:15:00Z"),
                1,
                "correlation-42",
                MESSAGE_ID
        );

        assertThatThrownBy(() -> ownershipService.persist(
                invalid,
                "order.ownership.v1",
                0,
                7L
        )).isInstanceOf(InvalidOrderOwnershipEventException.class);

        assertThat(repository.count()).isZero();
    }

    /**
     * Creates an ownership event for the fixed test order.
     *
     * @param customerId event customer identifier
     * @param messageId event identifier
     * @return ownership event fixture
     */
    private OrderOwnershipAssignedEvent ownershipEvent(UUID customerId, UUID messageId) {
        return new OrderOwnershipAssignedEvent(
                42L,
                customerId,
                Instant.parse("2026-10-27T10:15:00Z"),
                1,
                "correlation-42",
                messageId
        );
    }
}
