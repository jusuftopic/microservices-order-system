package org.example.inventoryservice.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Entity representing inventory state for a single product.
 *
 * <p>
 * This aggregate manages stock levels and reservations.
 * It is the source of truth for inventory availability.
 * </p>
 */

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {


    @Id
    private Long productId;

    /**
     * Total available stock that can be reserved.
     */
    private Integer availableQuantity;

    /**
     * Quantity reserved but not yet finalized.
     */
    private Integer reservedQuantity;


    /**
     * Checks whether enough stock is available for reservation.
     *
     * @param requestedQuantity quantity requested
     * @return true if reservation is possible
     */
    public boolean canReserve(int requestedQuantity) {
        return availableQuantity >= requestedQuantity;
    }


    /**
     * Reserves stock for an order.
     *
     * @param quantity quantity to reserve
     */
    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException(
                    "Not enough stock for product " + productId
            );
        }

        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }


    /**
     * Releases previously reserved stock (for compensation scenarios).
     */
    public void release(int quantity) {
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }
}
