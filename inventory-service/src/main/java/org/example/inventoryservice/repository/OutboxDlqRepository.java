package org.example.inventoryservice.repository;

import org.example.inventoryservice.entity.OutboxDlqEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Storage repository for persisted Outbox DLQ events
 */
public interface OutboxDlqRepository extends JpaRepository<OutboxDlqEvent, Long> {
}
