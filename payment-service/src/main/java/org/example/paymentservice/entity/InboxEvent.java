package org.example.paymentservice.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * InboxEvent represents a processed event in the Inbox Pattern.
 *
 * <p>
 * The Inbox Pattern ensures idempotent processing of incoming events
 * by storing already processed event IDs.
 * </p>
 *
 * <p>
 * If an event exists in this table, it has already been handled and
 * should NOT be processed again.
 * </p>
 */
@Entity
@Table(name = "inbox_event")
@Getter
@Setter
public class InboxEvent {

    /**
     * Unique identifier of the incoming event.
     */
    @Id
    @Column(name = "correlation_id", nullable = false, updatable = false)
    private String correlationId;

}
