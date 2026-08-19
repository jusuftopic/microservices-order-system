package org.example.messagingstarter.contracts.lifecycle;

import org.example.messagingstarter.EventConstants;

/**
 * Triggering event contracts and the services that originate them.
 */
public enum LifecycleTrigger {
    ORDER_STATUS_UPDATE(ServiceName.ORDER_SERVICE, "ORDER_STATUS_UPDATE"),
    INVENTORY_RESERVED(ServiceName.INVENTORY_SERVICE,
            EventConstants.EVENT_INVENTORY_RESERVED),
    INVENTORY_FAILED(ServiceName.INVENTORY_SERVICE,
            EventConstants.EVENT_INVENTORY_FAILED),
    PAYMENT_COMPLETED(ServiceName.PAYMENT_SERVICE,
            EventConstants.EVENT_PAYMENT_SUCCESS),
    PAYMENT_FAILED(ServiceName.PAYMENT_SERVICE,
            EventConstants.EVENT_PAYMENT_FAILED),
    INVENTORY_COMMIT_COMPLETED(ServiceName.INVENTORY_SERVICE,
            EventConstants.EVENT_INVENTORY_COMMIT_COMPLETED),
    INVENTORY_RELEASE_COMPLETED(ServiceName.INVENTORY_SERVICE,
            EventConstants.EVENT_INVENTORY_RELEASE_COMPLETED),
    INVENTORY_COMMIT_FAILED(ServiceName.INVENTORY_SERVICE,
            EventConstants.EVENT_INVENTORY_COMMIT_FAILED),
    ORDER_TIMEOUT_DETECTED(ServiceName.ORDER_SERVICE, "ORDER_TIMEOUT_DETECTED");

    private final ServiceName sourceService;
    private final String eventType;

    LifecycleTrigger(ServiceName sourceService, String eventType) {
        this.sourceService = sourceService;
        this.eventType = eventType;
    }

    /**
     * Returns the service that originates the triggering event.
     *
     * @return trigger source service
     */
    public ServiceName sourceService() {
        return sourceService;
    }

    /**
     * Returns the triggering event type written to lifecycle evidence.
     *
     * @return event contract type
     */
    public String eventType() {
        return eventType;
    }
}
