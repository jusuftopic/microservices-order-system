package org.example.orderservice.repository;

import org.example.orderservice.entity.OutboxDlqEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Storage repository for persisted Outbox DLQ events
 */
public interface OutboxDlqRepository extends JpaRepository<OutboxDlqEvent, Long> {
}
