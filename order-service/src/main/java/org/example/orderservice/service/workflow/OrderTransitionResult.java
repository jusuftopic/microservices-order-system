package org.example.orderservice.service.workflow;

import org.example.messagingstarter.contracts.lifecycle.OrderStatus;
import org.example.orderservice.entity.Order;

/**
 * Makes the outcome of an attempted workflow transition explicit so callers
 * do not execute downstream decisions after a skipped transition.
 */
public record OrderTransitionResult(
        Order order,
        OrderStatus previousStatus,
        OrderStatus targetStatus,
        boolean transitioned
) {
}
