package org.example.orderservice.service.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.example.orderservice.entity.Order;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.outbox.OrderOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.example.messagingstarter.EventConstants.EVENT_ORDER_LIFECYCLE_TRANSITIONED;

/**
 * Central place for order state transitions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderWorkflowService {

    private final OrderRepository repository;
    private final OrderOutboxService outboxService;

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS =
            Map.of(

                    OrderStatus.CREATED,
                    Set.of(
                            OrderStatus.INVENTORY_RESERVE_COMPLETED,
                            OrderStatus.INVENTORY_RESERVE_FAILED,
                            OrderStatus.TIMED_OUT
                    ),

                    OrderStatus.INVENTORY_RESERVE_COMPLETED,
                    Set.of(
                            OrderStatus.PAYMENT_COMPLETED,
                            OrderStatus.PAYMENT_FAILED,
                            OrderStatus.TIMED_OUT
                    ),

                    OrderStatus.PAYMENT_COMPLETED,
                    Set.of(
                            OrderStatus.INVENTORY_COMMIT_COMPLETED,
                            OrderStatus.INVENTORY_COMMIT_FAILED,
                            OrderStatus.TIMED_OUT
                    ),

                    OrderStatus.INVENTORY_COMMIT_COMPLETED,
                    Set.of(
                            OrderStatus.COMPLETED
                    ),

                    OrderStatus.INVENTORY_RESERVE_FAILED,
                    Set.of(OrderStatus.FAILED, OrderStatus.TIMED_OUT),

                    OrderStatus.PAYMENT_FAILED,
                    Set.of(OrderStatus.FAILED, OrderStatus.TIMED_OUT),

                    OrderStatus.INVENTORY_COMMIT_FAILED,
                    Set.of(OrderStatus.FAILED)
            );

    /**
     * Update order status to inventory processing
     *
     * @param orderId Order identifier to change status for
     * @param targetStatus Status to update order to
     * @return Updated {@link Order}
     */
    @Transactional
    public Order updateStatus(Long orderId, OrderStatus targetStatus) {
        return updateStatus(
                orderId,
                targetStatus,
                OrderTransitionContext.causedBy(
                        targetStatus.name(),
                        "ORDER_SERVICE",
                        "ORDER_STATUS_UPDATE",
                        null
                )
        );
    }

    @Transactional
    public Order updateStatus(
            Long orderId,
            OrderStatus targetStatus,
            OrderTransitionContext context
    ) {
        Order order = repository.findById(orderId)
                .orElseThrow();
        final OrderStatus currentStatus = order.getStatus();

        if (currentStatus.isFinalState()) return order;

        validateTransition(currentStatus, targetStatus);

        order.setStatus(targetStatus);
        final Order stored = repository.save(order);

        UUID lifecycleEventId = UUID.randomUUID();
        outboxService.storeEvent(
                stored.getId(),
                "ORDER",
                EVENT_ORDER_LIFECYCLE_TRANSITIONED,
                new OrderLifecycleTransitionedEvent(
                        stored.getId(),
                        currentStatus.name(),
                        targetStatus.name(),
                        context.reasonCode(),
                        context.sourceService(),
                        context.sourceEventType(),
                        context.causationId(),
                        context.orchestrationDecision(),
                        context.compensation(),
                        Instant.now(),
                        1,
                        stored.getCorrelationId(),
                        lifecycleEventId
                )
        );

        log.info("[ORDER-SERVICE][WORKFLOW] Order {} transitioned {} -> {}",
                stored.getId(), currentStatus, targetStatus);

        return stored;
    }

    private void validateTransition(OrderStatus current, OrderStatus target) {

        Set<OrderStatus> allowed =
                VALID_TRANSITIONS.getOrDefault(
                        current,
                        Collections.emptySet()
                );

        if (!allowed.contains(target)) {
            throw new IllegalStateException(
                    "Invalid order transition: " + current + " -> " + target
            );
        }
    }
}
