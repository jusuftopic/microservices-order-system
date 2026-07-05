package org.example.messagingstarter.inbox.repository;

import org.example.messagingstarter.inbox.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository responsible for InboxEvent persistence.
 *
 * <p>
 * Supports idempotent processing of incoming events.
 * </p>
 */
public interface InboxRepository extends JpaRepository<InboxEvent, UUID> {

    /**
     * Attempts to insert the given messageId into the inbox_event table.
     *
     * This method is used to implement the Inbox Pattern for Kafka message
     * deduplication. It ensures that each event is processed at most once
     * by relying on atomic unique constraint on the correlation_id column.
     *
     * @param messageId unique identifier of the consumed Kafka event
     * @return number of affected rows
     */
    @Modifying
    @Query(value = """
    INSERT INTO inbox_events (message_id)
    VALUES (:messageId)
    ON CONFLICT (message_id) DO NOTHING
    """, nativeQuery = true)
    int insertIfNotExists(@Param("messageId") UUID messageId);
}
