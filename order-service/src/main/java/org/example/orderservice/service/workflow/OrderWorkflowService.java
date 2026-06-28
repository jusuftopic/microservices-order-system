package org.example.orderservice.service.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.entity.Order;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Central place for order state transitions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderWorkflowService {

    private final OrderRepository repository;

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
        Order order = repository.findById(orderId)
                .orElseThrow();
        final OrderStatus currentStatus = order.getStatus();

        if (currentStatus.isFinalState()) return order;

        validateTransition(currentStatus, targetStatus);

        order.setStatus(targetStatus);
        final Order stored = repository.save(order);

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
