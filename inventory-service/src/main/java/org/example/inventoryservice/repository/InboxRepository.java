package org.example.inventoryservice.repository;

import org.example.inventoryservice.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository responsible for InboxEvent persistence.
 *
 * <p>
 * Supports idempotent processing of incoming events.
 * </p>
 */
@Repository
public interface InboxRepository extends JpaRepository<InboxEvent, String> {

    /**
     * Attempts to insert the given correlationId into the inbox_event table.
     *
     * This method is used to implement the Inbox Pattern for Kafka message
     * deduplication. It ensures that each event is processed at most once
     * by relying on atomic unique constraint on the correlation_id column.
     *
     * @param correlationId unique identifier of the consumed Kafka event
     * @return number of affected rows
     */
    @Modifying
    @Query(value = """
    INSERT INTO inbox_events (correlation_id)
    VALUES (:correlationId)
    ON CONFLICT (correlation_id) DO NOTHING
    """, nativeQuery = true)
    int insertIfNotExists(@Param("correlationId") String correlationId);
}
