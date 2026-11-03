package org.example.orderservice.repository;

import jakarta.persistence.LockModeType;
import org.example.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Checks whether the identified order belongs to the authenticated customer.
     *
     * @param orderId order identifier
     * @param customerId internal customer identifier
     * @return {@code true} only for an exact order and customer match
     */
    boolean existsByIdAndCustomerId(Long orderId, UUID customerId);


    @Query("""
            SELECT o.id
            FROM Order o
            WHERE o.status NOT IN (
                org.example.messagingstarter.contracts.lifecycle.OrderStatus.COMPLETED,
                org.example.messagingstarter.contracts.lifecycle.OrderStatus.FAILED,
                org.example.messagingstarter.contracts.lifecycle.OrderStatus.TIMED_OUT
            )
            AND COALESCE(o.updatedAt, o.createdAt) < :threshold
            """)
    List<Long> findTimedOutOrderIds(@Param("threshold") LocalDateTime threshold);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

}
