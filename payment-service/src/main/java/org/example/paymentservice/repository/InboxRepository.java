package org.example.paymentservice.repository;

import org.example.paymentservice.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for Inbox persistence operations.
 */
public interface InboxRepository extends JpaRepository<InboxEvent, UUID> {
}
