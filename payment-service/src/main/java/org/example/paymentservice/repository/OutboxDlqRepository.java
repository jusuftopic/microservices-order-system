package org.example.paymentservice.repository;

import org.example.paymentservice.entity.OutboxDlqEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Storage repository for persisted Outbox DLQ events
 */
@Repository
public interface OutboxDlqRepository extends JpaRepository<OutboxDlqEvent, Long> {
}
