package org.example.messagingstarter.outbox.repository;

import org.example.messagingstarter.outbox.entity.OutboxDlqEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Storage repository for persisted Outbox DLQ events
 */
public interface OutboxDlqRepository extends JpaRepository<OutboxDlqEvent, Long> {
}
