package org.example.commons.event.utils;

import org.example.commons.event.EventConstants;

import static org.example.commons.event.EventConstants.*;

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
                    EventConstants.TOPIC_ORDER_PAYMENT_REQUEST_V1;

            case EventConstants.EVENT_INVENTORY_RESERVED,
                 EventConstants.EVENT_INVENTORY_FAILED-> EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1;

            case EventConstants.EVENT_PAYMENT_SUCCESS,
                 EventConstants.EVENT_PAYMENT_FAILED-> EventConstants.TOPIC_ORDER_PAYMENT_RESPONSE_V1;

            case EventConstants.EVENT_INVENTORY_COMMIT_REQUESTED,
                 EventConstants.EVENT_INVENTORY_RELEASE_REQUESTED ->
                    TOPIC_ORDER_INVENTORY_FINALIZATION_REQUEST_V1;

            case EventConstants.EVENT_INVENTORY_COMMIT_COMPLETED,
                 EventConstants.EVENT_INVENTORY_COMMIT_FAILED,
                 EventConstants.EVENT_INVENTORY_RELEASE_COMPLETED ->
                    TOPIC_ORDER_INVENTORY_FINALIZATION_RESPONSE_V1;

            case EventConstants.EVENT_NOTIFICATION_REQUESTED ->
                TOPIC_NOTIFICATION_REQUEST_V1;

            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + eventType
            );
        };
    }
}
