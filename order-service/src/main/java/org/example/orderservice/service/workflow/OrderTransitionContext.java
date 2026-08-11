package org.example.orderservice.service.workflow;

import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent.Compensation;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent.OrchestrationDecision;

import java.util.UUID;

/**
 * Evidence attached to an order lifecycle transition.
 *
 * <p>The context records why the Order Service made the transition and which
 * action it selected next. It never records a downstream action as completed.</p>
 */
public record OrderTransitionContext(
        String reasonCode,
        String sourceService,
        String sourceEventType,
        UUID causationId,
        OrchestrationDecision orchestrationDecision,
        Compensation compensation
) {

    public static OrderTransitionContext causedBy(
            String reasonCode,
            String sourceService,
            String sourceEventType,
            UUID causationId
    ) {
        return new OrderTransitionContext(
                reasonCode,
                sourceService,
                sourceEventType,
                causationId,
                null,
                Compensation.notRequired()
        );
    }

    public OrderTransitionContext withDecision(
            String code,
            String targetService,
            UUID commandId
    ) {
        return new OrderTransitionContext(
                reasonCode,
                sourceService,
                sourceEventType,
                causationId,
                new OrchestrationDecision(code, targetService, commandId),
                compensation
        );
    }

    public OrderTransitionContext withCompensation(String type) {
        return new OrderTransitionContext(
                reasonCode,
                sourceService,
                sourceEventType,
                causationId,
                orchestrationDecision,
                new Compensation(true, type)
        );
    }
}
