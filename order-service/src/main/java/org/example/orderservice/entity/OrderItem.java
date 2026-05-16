package org.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Order entity represents a purchase/order item in the system.
 *
 * Table: order_items
 */

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Product reference (from catalog)
     */
    @Column(nullable = false)
    private Long productId;

    /**
     * Quantity ordered
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Owning order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

}
