package org.example.orderservice.repository;

import org.example.orderservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Storage repository for persisted Outbox events
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find all outbox events needed to be sent to Kafka
     *
     * @return List of all unprocessed {@link OutboxEvent} retrieved by FIFO.
     */
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
}
