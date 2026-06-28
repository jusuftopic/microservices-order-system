package org.example.orderservice.repository;

import org.example.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {


    @Query("""
            SELECT o
            FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.status NOT IN (
                org.example.orderservice.enums.OrderStatus.COMPLETED,
                org.example.orderservice.enums.OrderStatus.FAILED,
                org.example.orderservice.enums.OrderStatus.TIMED_OUT
            )
            AND COALESCE(o.updatedAt, o.createdAt) < :threshold
            """)
    List<Order> findTimedOutOrders(LocalDateTime threshold);

}
