package org.example.orderservice.service.workflow;

import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent.Compensation;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent.OrchestrationDecision;
import org.example.messagingstarter.contracts.lifecycle.CompensationType;
import org.example.messagingstarter.contracts.lifecycle.LifecycleReasonCode;
import org.example.messagingstarter.contracts.lifecycle.LifecycleTrigger;
import org.example.messagingstarter.contracts.lifecycle.OrchestrationDecisionCode;

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

    /**
     * Creates transition evidence from a known lifecycle reason and triggering event.
     *
     * @param reasonCode business reason for the transition
     * @param trigger triggering event and its source service
     * @param causationId triggering event identifier
     * @return transition context without a subsequent orchestration decision
     */
    public static OrderTransitionContext causedBy(
            LifecycleReasonCode reasonCode,
            LifecycleTrigger trigger,
            UUID causationId
    ) {
        return new OrderTransitionContext(
                reasonCode.code(),
                trigger.sourceService().code(),
                trigger.eventType(),
                causationId,
                null,
                Compensation.notRequired()
        );
    }

    /**
     * Adds the next orchestration action selected by the Order Service.
     *
     * @param decision selected action and its defined target service
     * @param commandId identifier of the command created for the action
     * @return transition context containing the decision
     */
    public OrderTransitionContext withDecision(
            OrchestrationDecisionCode decision,
            UUID commandId
    ) {
        return new OrderTransitionContext(
                reasonCode,
                sourceService,
                sourceEventType,
                causationId,
                new OrchestrationDecision(
                        decision.code(),
                        decision.targetService().code(),
                        commandId
                ),
                compensation
        );
    }

    /**
     * Marks the transition as requiring a known compensation action.
     *
     * @param type compensation action
     * @return transition context containing compensation evidence
     */
    public OrderTransitionContext withCompensation(CompensationType type) {
        return new OrderTransitionContext(
                reasonCode,
                sourceService,
                sourceEventType,
                causationId,
                orchestrationDecision,
                new Compensation(true, type.code())
        );
    }
}
