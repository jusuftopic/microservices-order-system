package org.example.inventoryservice.repository;

import org.example.inventoryservice.entity.OutboxDlqEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Storage repository for persisted Outbox DLQ events
 */
@Repository
public interface OutboxDlqRepository extends JpaRepository<OutboxDlqEvent, Long> {
}
