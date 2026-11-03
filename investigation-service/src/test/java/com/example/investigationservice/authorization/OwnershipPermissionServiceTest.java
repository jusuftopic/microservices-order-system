package com.example.investigationservice.authorization;

import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.repository.OrderOwnerRepository;
import com.example.investigationservice.repository.QuestionExecutionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifies Investigation ownership decisions and denial metrics.
 */
class OwnershipPermissionServiceTest {

    private static final long ORDER_ID = 42L;
    private static final UUID CUSTOMER_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID EXECUTION_ID =
            UUID.fromString("55555555-5555-4555-8555-555555555555");

    private final OrderOwnerRepository ownerRepository = mock(OrderOwnerRepository.class);
    private final QuestionExecutionRepository executionRepository = mock(QuestionExecutionRepository.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OwnershipPermissionService service = new OwnershipPermissionService(
            ownerRepository, executionRepository, new InvestigationMetrics(registry));

    /**
     * Allows an exact local order ownership match.
     */
    @Test
    void allowsOrderOwner() {
        when(ownerRepository.existsByOrderIdAndCustomerId(ORDER_ID, CUSTOMER_ID)).thenReturn(true);

        assertThat(service.canAccessOrder(ORDER_ID, CUSTOMER_ID.toString())).isTrue();

        assertThat(deniedCount()).isZero();

        verify(ownerRepository).existsByOrderIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
        verifyNoMoreInteractions(ownerRepository);
    }

    /**
     * Denies a missing ownership projection and records a denial.
     */
    @Test
    void deniesOrderWithoutMatchingProjection() {
        when(ownerRepository.existsByOrderIdAndCustomerId(ORDER_ID, CUSTOMER_ID))
                .thenReturn(false);

        assertThat(service.canAccessOrder(ORDER_ID, CUSTOMER_ID.toString())).isFalse();

        assertThat(deniedCount()).isOne();

        verify(ownerRepository).existsByOrderIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
        verifyNoMoreInteractions(ownerRepository);
    }

    /**
     * Allows a question only through the ownership of its associated order.
     */
    @Test
    void allowsQuestionForOrderOwner() {
        when(executionRepository.findOrderIdByExecutionId(EXECUTION_ID))
                .thenReturn(Optional.of(ORDER_ID));
        when(ownerRepository.existsByOrderIdAndCustomerId(ORDER_ID, CUSTOMER_ID))
                .thenReturn(true);

        assertThat(service.canAccessQuestion(EXECUTION_ID, CUSTOMER_ID.toString())).isTrue();
    }

    /**
     * Denies an unknown question without performing an ownership lookup.
     */
    @Test
    void deniesUnknownQuestionExecution() {
        when(executionRepository.findOrderIdByExecutionId(EXECUTION_ID))
                .thenReturn(Optional.empty());

        assertThat(service.canAccessQuestion(EXECUTION_ID, CUSTOMER_ID.toString())).isFalse();

        verify(ownerRepository, never())
                .existsByOrderIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
        assertThat(deniedCount()).isOne();
    }

    /**
     * Denies malformed identity before querying the ownership projection.
     */
    @Test
    void deniesMalformedCustomerIdentity() {
        assertThat(service.canAccessOrder(ORDER_ID, "not-a-uuid")).isFalse();

        verify(ownerRepository, never())
                .existsByOrderIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
        assertThat(deniedCount()).isOne();
    }

    /**
     * Reads the total denial counter across its bounded tag values.
     *
     * @return total recorded authorization denials
     */
    private double deniedCount() {
        return registry.find("investigation.authorization.denied.total")
                .counters()
                .stream()
                .mapToDouble(counter -> counter.count())
                .sum();
    }
}
