package org.example.commons.event.utils;

import org.example.commons.event.EventConstants;

/**
 * Utils class help to resolve topic based on happened event type
 */
public class TopicResolver {

    /**
     * Maps occurred event to the corresponding topic
     *
     * @param eventType Occurred event
     * @return mapped topic
     */
    public static String resolveTopic(String eventType) {
        return switch (eventType) {
            case EventConstants.EVENT_INVENTORY_CHECK_REQUESTED ->
                    EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1;

            case EventConstants.EVENT_PAYMENT_REQUESTED ->
                    EventConstants.TOPIC_ODER_PAYMENT_REQUEST_V1;

            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + eventType
            );
        };

    }
}
