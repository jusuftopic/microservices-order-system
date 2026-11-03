package org.example.orderservice.unit.authorization;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.example.orderservice.authorization.OwnershipPermissionService;
import org.example.orderservice.metrics.OrderMetrics;
import org.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifies Order Service ownership decisions and denial metrics.
 */
class OwnershipPermissionServiceTest {

    private static final long ORDER_ID = 42L;
    private static final UUID CUSTOMER_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");

    private final OrderRepository repository = mock(OrderRepository.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OwnershipPermissionService service = new OwnershipPermissionService(
            repository,
            new OrderMetrics(registry)
    );

    /**
     * Allows an exact order and authenticated customer match.
     */
    @Test
    void allowsOrderOwner() {
        when(repository.existsByIdAndCustomerId(ORDER_ID, CUSTOMER_ID)).thenReturn(true);

        assertThat(service.canAccess(ORDER_ID, CUSTOMER_ID.toString())).isTrue();

        assertThat(deniedCount()).isZero();

        verify(repository).existsByIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
        verifyNoMoreInteractions(repository);

    }

    /**
     * Denies a different customer and records a bounded metric.
     */
    @Test
    void deniesCustomerThatDoesNotOwnOrder() {
        when(repository.existsByIdAndCustomerId(ORDER_ID, CUSTOMER_ID)).thenReturn(false);

        assertThat(service.canAccess(ORDER_ID, CUSTOMER_ID.toString())).isFalse();

        assertThat(deniedCount()).isOne();

        verify(repository).existsByIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
        verifyNoMoreInteractions(repository);
    }

    /**
     * Denies a malformed identity without querying ownership data.
     */
    @Test
    void deniesMalformedCustomerIdentity() {
        assertThat(service.canAccess(ORDER_ID, "not-a-uuid")).isFalse();

        verify(repository, never()).existsByIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
        assertThat(deniedCount()).isOne();
    }

    /**
     * Reads the total denial counter across its bounded tag values.
     *
     * @return total recorded authorization denials
     */
    private double deniedCount() {
        return registry.find("orders.authorization.denied.total")
                .counters()
                .stream()
                .mapToDouble(counter -> counter.count())
                .sum();
    }
}
