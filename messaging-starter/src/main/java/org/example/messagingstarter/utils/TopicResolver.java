package org.example.messagingstarter.utils;

import static org.example.messagingstarter.EventConstants.*;

/**
 * Utils class help to resolve topic based on happened event types
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
            case EVENT_INVENTORY_CHECK_REQUESTED ->
                    TOPIC_ORDER_INVENTORY_REQUEST_V1;

            case EVENT_PAYMENT_REQUESTED, EVENT_PAYMENT_REFUND_REQUESTED ->
                    TOPIC_ORDER_PAYMENT_REQUEST_V1;

            case EVENT_INVENTORY_RESERVED, EVENT_INVENTORY_FAILED ->
                    TOPIC_ORDER_INVENTORY_RESPONSE_V1;

            case EVENT_PAYMENT_SUCCESS, EVENT_PAYMENT_FAILED ->
                    TOPIC_ORDER_PAYMENT_RESPONSE_V1;

            case EVENT_INVENTORY_COMMIT_REQUESTED, EVENT_INVENTORY_RELEASE_REQUESTED
                    -> TOPIC_ORDER_INVENTORY_FINALIZATION_REQUEST_V1;

            case EVENT_INVENTORY_COMMIT_COMPLETED, EVENT_INVENTORY_COMMIT_FAILED,
                 EVENT_INVENTORY_RELEASE_COMPLETED ->
                    TOPIC_ORDER_INVENTORY_FINALIZATION_RESPONSE_V1;

            case EVENT_NOTIFICATION_REQUESTED ->
                TOPIC_NOTIFICATION_REQUEST_V1;

            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + eventType
            );
        };
    }
}
