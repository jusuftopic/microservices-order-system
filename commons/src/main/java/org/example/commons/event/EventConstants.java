package org.example.commons.event;

/**
 * Class holds well-known event constants
 */
public class EventConstants {

    /* EVENT TYPES */
    public static final String EVENT_PAYMENT_REQUESTED = "PAYMENT_REQUESTED";
    public static final String EVENT_INVENTORY_CHECK_REQUESTED = "INVENTORY_CHECK_REQUESTED";
    public static final String EVENT_INVENTORY_RESERVED = "EVENT_INVENTORY_RESERVED";
    public static final String EVENT_INVENTORY_FAILED = "EVENT_INVENTORY_FAILED";
    public static final String EVENT_PAYMENT_SUCCESS = "EVENT_PAYMENT_SUCCESS";
    public static final String EVENT_PAYMENT_FAILED = "EVENT_PAYMENT_FAILED";
    public static final String EVENT_INVENTORY_COMMIT_REQUESTED = "EVENT_INVENTORY_COMMIT_REQUESTED";
    public static final String EVENT_INVENTORY_RELEASE_REQUESTED = "EVENT_INVENTORY_RELEASE_REQUESTED";
    public static final String EVENT_INVENTORY_COMMIT_COMPLETED = "EVENT_INVENTORY_COMMIT_COMPLETED";
    public static final String EVENT_INVENTORY_COMMIT_FAILED = "EVENT_INVENTORY_COMMIT_FAILED";
    public static final String EVENT_INVENTORY_RELEASE_COMPLETED = "EVENT_INVENTORY_RELEASE_COMPLETED";
    public static final String EVENT_NOTIFICATION_REQUESTED = "EVENT_NOTIFICATION_REQUESTED";
    public static final String EVENT_PAYMENT_REFUND_REQUESTED = "EVENT_PAYMENT_REFUND_REQUESTED";

    /* TOPICS */
    public static final String TOPIC_ORDER_INVENTORY_REQUEST_V1 = "order.inventory.request.v1";
    public static final String TOPIC_ORDER_INVENTORY_RESPONSE_V1 = "order.inventory.response.v1";
    public static final String TOPIC_ORDER_PAYMENT_REQUEST_V1 = "order.payment.request.v1";
    public static final String TOPIC_ORDER_PAYMENT_RESPONSE_V1 = "order.payment.response.v1";
    public static final String TOPIC_ORDER_INVENTORY_FINALIZATION_REQUEST_V1 = "order.inventory.finalization.request.v1";
    public static final String TOPIC_ORDER_INVENTORY_FINALIZATION_RESPONSE_V1 = "order.inventory.finalization.response.v1";
    public static final String TOPIC_NOTIFICATION_REQUEST_V1 = "notification.request.v1";

    /* TOPICS - DLQ */
    public static final String TOPIC_ORDER_DLQ = "order.dlq";
    public static final String TOPIC_INVENTORY_DLQ = "inventory.dlq";
    public static final String TOPIC_PAYMENT_DLQ = "payment.dlq";

}
