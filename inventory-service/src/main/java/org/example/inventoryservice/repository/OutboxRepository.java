package org.example.inventoryservice.repository;

import org.example.inventoryservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing outbox events.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {


    /**
     * Finds all unprocessed events ordered by creation time.
     */
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();

}
