package org.example.commons.event.contracts;

import java.util.UUID;


/**
 * Event emitted to indicate failed inventory commit.
 *
 * <p>
 * This event represents an unsuccessful finalization of the inventory step
 * in the order workflow. It signals that previously reserved inventory
 * could not be permanently deducted.
 * </p>
 *
 * <p>
 * Consumed by Order Service to trigger compensation logic (e.g. refund or notifications).
 * </p>
 */
public record InventoryCommitFailedEvent(

      /**
      * Unique identifier of the order.
       */
      Long orderId,

       /**
       * Reason describing why the commit operation failed.
       */
      String reason,

       /**
       * Correlation identifier used for tracking the workflow across services.
       */
      String correlationId,

       /**
       * Unique identifier of the message.
       */
      UUID messageId
) implements BaseEvent {
}
