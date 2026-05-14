package org.example.paymentservice.repository;

import org.example.paymentservice.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repository for Inbox persistence operations.
 */
public interface InboxRepository extends JpaRepository<InboxEvent, UUID> {

    /**
     * Attempts to insert the given eventId into the inbox_event table.
     *
     * This method is used to implement the Inbox Pattern for Kafka message
     * deduplication. It ensures that each event is processed at most once
     * by relying on atomic unique constraint on the event_id column.
     *
     * @param eventId unique identifier of the consumed Kafka event
     * @return number of affected rows
     */
    @Modifying
    @Query(value = """
    INSERT INTO inbox_event (event_id)
    VALUES (:eventId)
    ON CONFLICT (event_id) DO NOTHING
    """, nativeQuery = true)
    int insertIfNotExists(@Param("eventId") UUID eventId);
}
