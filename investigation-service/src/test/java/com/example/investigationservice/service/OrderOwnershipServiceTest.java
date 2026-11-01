package com.example.investigationservice.service;

import com.example.investigationservice.entity.OrderOwner;
import com.example.investigationservice.exception.InvalidOrderOwnershipEventException;
import com.example.investigationservice.repository.OrderOwnerRepository;
import com.example.investigationservice.validation.OrderOwnershipEventValidator;
import org.example.messagingstarter.contracts.events.OrderOwnershipAssignedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies ownership projection persistence and fail-closed access decisions.
 */
class OrderOwnershipServiceTest {

    private static final long ORDER_ID = 42L;
    private static final UUID CUSTOMER_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID MESSAGE_ID =
            UUID.fromString("2b997737-7918-442b-bbd8-c26e52fce083");
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-27T10:15:00Z");

    private final OrderOwnerRepository repository = mock(OrderOwnerRepository.class);
    private final OrderOwnershipEventValidator validator =
            mock(OrderOwnershipEventValidator.class);
    private final OrderOwnershipService service =
            new OrderOwnershipService(repository, validator);

    /**
     * Verifies that a new event is validated, mapped, and persisted.
     */
    @Test
    void persistsNewOwnershipAssignment() {
        OrderOwnershipAssignedEvent event = ownershipEvent(CUSTOMER_ID, MESSAGE_ID);
        when(repository.findByMessageId(MESSAGE_ID)).thenReturn(Optional.empty());
        when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());

        boolean persisted = service.persist(event, "order.ownership.v1", 1, 7L);

        assertThat(persisted).isTrue();
        verify(validator).validate(event, "order.ownership.v1", 1, 7L);
        ArgumentCaptor<OrderOwner> ownerCaptor = ArgumentCaptor.forClass(OrderOwner.class);
        verify(repository).saveAndFlush(ownerCaptor.capture());
        assertThat(ownerCaptor.getValue()).satisfies(owner -> {
            assertThat(owner.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(owner.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(owner.getMessageId()).isEqualTo(MESSAGE_ID);
            assertThat(owner.getOccurredAt()).isEqualTo(OCCURRED_AT);
            assertThat(owner.getEventVersion()).isOne();
            assertThat(owner.getCorrelationId()).isEqualTo("correlation-42");
        });
    }

    /**
     * Verifies that an exact message redelivery does not write another row.
     */
    @Test
    void ignoresExactMessageRedelivery() {
        OrderOwnershipAssignedEvent event = ownershipEvent(CUSTOMER_ID, MESSAGE_ID);
        when(repository.findByMessageId(MESSAGE_ID)).thenReturn(Optional.of(owner()));

        assertThat(service.persist(event, "order.ownership.v1", 0, 7L)).isFalse();

        verify(repository, never()).saveAndFlush(any(OrderOwner.class));
        verify(repository, never()).findById(ORDER_ID);
    }

    /**
     * Verifies that a later event cannot replace an existing owner.
     */
    @Test
    void rejectsConflictingAssignment() {
        UUID differentCustomer =
                UUID.fromString("33333333-3333-4333-8333-333333333333");
        OrderOwnershipAssignedEvent event = ownershipEvent(
                differentCustomer,
                UUID.fromString("44444444-4444-4444-8444-444444444444")
        );
        when(repository.findByMessageId(event.messageId())).thenReturn(Optional.empty());
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(owner()));

        assertThatThrownBy(() -> service.persist(event, "order.ownership.v1", 0, 8L))
                .isInstanceOf(InvalidOrderOwnershipEventException.class)
                .hasMessage("Order ownership assignment conflicts with an existing projection");

        verify(repository, never()).saveAndFlush(any(OrderOwner.class));
    }

    /**
     * Verifies that an exact assignment inserted concurrently is idempotent.
     */
    @Test
    void handlesConcurrentExactInsertAsRedelivery() {
        OrderOwnershipAssignedEvent event = ownershipEvent(CUSTOMER_ID, MESSAGE_ID);
        when(repository.findByMessageId(MESSAGE_ID)).thenReturn(Optional.empty());
        when(repository.findById(ORDER_ID))
                .thenReturn(Optional.empty(), Optional.of(owner()));
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(repository).saveAndFlush(any(OrderOwner.class));

        assertThat(service.persist(event, "order.ownership.v1", 0, 7L)).isFalse();
    }

    /**
     * Verifies that unrelated database constraint failures remain retryable.
     */
    @Test
    void propagatesUnresolvedDataIntegrityViolation() {
        OrderOwnershipAssignedEvent event = ownershipEvent(CUSTOMER_ID, MESSAGE_ID);
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("unresolved");
        when(repository.findByMessageId(MESSAGE_ID)).thenReturn(Optional.empty());
        when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());
        doThrow(failure).when(repository).saveAndFlush(any(OrderOwner.class));

        assertThatThrownBy(() -> service.persist(event, "order.ownership.v1", 0, 7L))
                .isSameAs(failure);
    }

    /**
     * Verifies exact repository lookup for valid ownership input.
     */
    @Test
    void checksValidOwnershipAgainstRepository() {
        when(repository.existsByOrderIdAndCustomerId(ORDER_ID, CUSTOMER_ID))
                .thenReturn(true);

        assertThat(service.isOwner(ORDER_ID, CUSTOMER_ID)).isTrue();

        verify(repository).existsByOrderIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
    }

    /**
     * Verifies that invalid ownership input is denied without database access.
     */
    @Test
    void deniesInvalidOwnershipInputWithoutRepositoryLookup() {
        assertThat(service.isOwner(null, CUSTOMER_ID)).isFalse();
        assertThat(service.isOwner(0L, CUSTOMER_ID)).isFalse();
        assertThat(service.isOwner(-1L, CUSTOMER_ID)).isFalse();
        assertThat(service.isOwner(ORDER_ID, null)).isFalse();

        verifyNoInteractions(repository);
    }

    /**
     * Creates the stored ownership projection used by duplicate checks.
     *
     * @return ownership projection fixture
     */
    private OrderOwner owner() {
        return OrderOwner.builder()
                .orderId(ORDER_ID)
                .customerId(CUSTOMER_ID)
                .messageId(MESSAGE_ID)
                .occurredAt(OCCURRED_AT)
                .eventVersion(1)
                .correlationId("correlation-42")
                .build();
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
                ORDER_ID,
                customerId,
                OCCURRED_AT,
                1,
                "correlation-42",
                messageId
        );
    }
}
