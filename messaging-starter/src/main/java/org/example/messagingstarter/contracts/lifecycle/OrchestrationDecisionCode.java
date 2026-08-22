package org.example.messagingstarter.contracts.lifecycle;

import java.util.Arrays;
import java.util.Optional;

/**
 * Actions the Order Service can select after a lifecycle transition.
 */
public enum OrchestrationDecisionCode {
    PROCESS_PAYMENT(ServiceName.PAYMENT_SERVICE),
    COMMIT_INVENTORY(ServiceName.INVENTORY_SERVICE),
    RELEASE_INVENTORY(ServiceName.INVENTORY_SERVICE),
    SEND_NOTIFICATION(ServiceName.NOTIFICATION_SERVICE),
    REFUND_PAYMENT(ServiceName.PAYMENT_SERVICE);

    private final ServiceName targetService;

    OrchestrationDecisionCode(ServiceName targetService) {
        this.targetService = targetService;
    }

    /**
     * Returns the value written to the lifecycle event contract.
     *
     * @return stable decision code
     */
    public String code() {
        return name();
    }

    /**
     * Returns the service responsible for executing this decision.
     *
     * @return decision target service
     */
    public ServiceName targetService() {
        return targetService;
    }

    /**
     * Resolves a known decision without rejecting newer contract values.
     *
     * @param code serialized decision code
     * @return matching decision, when known by this application version
     */
    public static Optional<OrchestrationDecisionCode> fromCode(String code) {
        return Arrays.stream(values())
                .filter(decision -> decision.code().equals(code))
                .findFirst();
    }
}
