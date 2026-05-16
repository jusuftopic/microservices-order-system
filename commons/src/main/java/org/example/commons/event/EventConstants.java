package org.example.commons.event;

/**
 * Class holds well-known event constants
 */
public class EventConstants {

    /* EVENT TYPES */
    public static final String EVENT_PAYMENT_REQUESTED = "PAYMENT_REQUESTED";

    /* TOPICS */
    public static final String TOPIC_ORDER_INVENTORY_REQUEST_V1 = "order.inventory.request.v1";
    public static final String TOPIC_ORDER_INVENTORY_RESPONSE_V1 = "order.inventory.response.v1";
    public static final String TOPIC_ODER_PAYMENT_REQUEST_V1 = "order.payment.request.v1";
    public static final String TOPIC_ORDER_PAYMENT_RESPONSE_V1 = "order.payment.response.v1";

    public static final String TOPIC_ORDER_DLQ = "order.dlq";
    public static final String TOPIC_INVENTORY_DLQ = "inventory.dlq";
    public static final String TOPIC_PAYMENT_DLQ = "payment.dlq";

}
