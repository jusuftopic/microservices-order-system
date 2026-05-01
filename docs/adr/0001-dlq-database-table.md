# ADR 0001: Use Database-based DLQ for Outbox Failure Handling

## Status
Accepted

## Context

The system uses the Outbox pattern to reliably publish domain events from the order-service to Kafka.

While implementing the Outbox publisher, we identified a failure scenario where certain events may consistently fail to be published due to:

- Invalid payload structure
- Serialization issues
- Business logic inconsistencies in event creation
- Persistent infrastructure misconfiguration
- Poison messages that will never succeed on retry

Without proper handling, these events would remain in the Outbox indefinitely and continuously be retried, causing:
- unnecessary system load
- log noise
- blocked visibility into real failures
- inability to inspect and debug problematic events

## Decision

We introduce a **Database-based Dead Letter Queue (DLQ)** table.

When an Outbox event exceeds the maximum retry threshold, it is moved to the DLQ table instead of being retried indefinitely.

The DLQ acts as a persistent storage for failed events that:
- could not be published to Kafka
- require manual inspection or intervention
- may be replayed after fixing underlying issues

## Consequences

### Positive
- Prevents infinite retry loops
- Improves system observability
- Enables debugging of failed events
- Provides manual replay capability
- Keeps Outbox table clean and focused on active events

### Negative
- Requires additional storage (DLQ table)
- Introduces manual or separate process for replay
- Slightly increases system complexity

## Alternatives considered

### 1. Kafka Dead Letter Topic (DLT)
Rejected for this stage of the project due to:
- external dependency on Kafka tooling for inspection
- reduced visibility for debugging in database-centric workflow

### 2. Infinite retry with exponential backoff
Rejected because:
- poison messages would still block system resources
- no clear failure boundary

## Outcome

A DLQ table is introduced as part of the Outbox system. Events exceeding retry limits are moved into this table for analysis and potential replay.