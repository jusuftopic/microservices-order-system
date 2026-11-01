package com.example.investigationservice.listener.kafka;

import com.example.investigationservice.exception.InvalidOrderOwnershipEventException;
import com.example.investigationservice.service.OrderOwnershipService;
import org.example.messagingstarter.contracts.events.OrderOwnershipAssignedEvent;
import org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies ownership listener delegation and reliability configuration.
 */
class OrderOwnershipKafkaListenerTest {

    private final OrderOwnershipService ownershipService = mock(OrderOwnershipService.class);
    private final OrderOwnershipKafkaListener listener =
            new OrderOwnershipKafkaListener(ownershipService);

    /**
     * Verifies that a valid ownership event is delegated with Kafka coordinates.
     */
    @Test
    void acceptsOwnershipAssignment() {
        OrderOwnershipAssignedEvent event = ownershipEvent();

        listener.handleOrderOwnershipAssigned(event, "order.ownership.v1", 0, 7L);

        verify(ownershipService).persist(event, "order.ownership.v1", 0, 7L);
    }

    /**
     * Verifies that unmatched payloads enter the configured error-handling path.
     */
    @Test
    void rejectsUnknownPayload() {
        assertThatThrownBy(() -> listener.handleUnknownObject("unexpected-event"))
                .isInstanceOf(InvalidOrderOwnershipEventException.class);
    }

    /**
     * Prevents the listener from accidentally bypassing shared retry and DLT handling.
     */
    @Test
    void usesBusinessListenerFactory() {
        KafkaListener annotation = OrderOwnershipKafkaListener.class
                .getAnnotation(KafkaListener.class);

        assertThat(annotation.containerFactory())
                .isEqualTo(KafkaConsumerReliabilitySupport.BUSINESS_LISTENER_FACTORY);
    }

    /**
     * Creates a valid ownership event fixture.
     *
     * @return ownership event fixture
     */
    private OrderOwnershipAssignedEvent ownershipEvent() {
        return new OrderOwnershipAssignedEvent(
                42L,
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                Instant.parse("2026-10-27T10:15:00Z"),
                1,
                "correlation-42",
                UUID.fromString("2b997737-7918-442b-bbd8-c26e52fce083")
        );
    }
}
