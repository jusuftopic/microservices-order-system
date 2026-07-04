package org.example.orderservice.repository;

import org.example.orderservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Storage repository for persisted Outbox events
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
    SELECT e
    FROM OutboxEvent e
    WHERE e.processed = false
      AND (
            e.nextRetryAt IS NULL
            OR e.nextRetryAt <= :now
          )
    ORDER BY e.createdAt ASC
    """)
    List<OutboxEvent> findReadyForPublishing(LocalDateTime now);

}
