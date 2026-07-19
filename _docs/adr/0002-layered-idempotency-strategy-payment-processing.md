# ADR 0002: Reliable Event Processing with Inbox and Outbox Patterns

## Status
Accepted

---

## Context

The system uses asynchronous, event-driven communication between independently deployed services.

Kafka provides at-least-once message delivery. Consequently, the architecture must assume that:

* the same message may be delivered more than once
* a service may stop after updating its database but before acknowledging a Kafka record
* a service may stop after creating business data but before publishing the corresponding event
* Kafka may be temporarily unavailable
* message publication may fail after the local business transaction has completed
* consumers may process the same business operation concurrently
* services may be restarted during deployments or infrastructure failures

Without additional safeguards, these conditions could cause:

* duplicate business operations
* lost integration events
* inconsistent service state
* events being published without their corresponding business changes
* business changes being committed without the required event being published

The architecture therefore requires a consistent reliability model that can be applied across all services rather than implementing service-specific message handling strategies.

---

## Decision

All services that participate in asynchronous business workflows use the Inbox and Outbox patterns.

The two patterns protect opposite sides of message processing:

* the Inbox pattern protects consumers against duplicate message processing
* the Outbox pattern protects producers against lost events and dual-write inconsistencies

Together, they provide reliable event processing while preserving local transactional consistency.

---

## Inbox Pattern

Each consumed Kafka event contains a unique message identifier.

Before applying the event's business operation, the consumer attempts to insert that identifier into its local Inbox table.

The Inbox record and the resulting business state changes are persisted within the same database transaction.

If the message identifier already exists, the event has previously been processed and the duplicate delivery is ignored.

The Inbox pattern provides:

* protection against Kafka redelivery
* idempotent consumer behavior
* safe recovery after service restarts
* prevention of repeated business state transitions
* a local record of processed messages

A typical consumer transaction follows this sequence:

```text
Kafka event received
        ↓
Insert message identifier into Inbox
        ↓
Duplicate identifier?
   ├── Yes → skip business processing
   └── No  → apply business changes
                    ↓
             create Outbox event if required
                    ↓
             commit local transaction
```

The atomic Inbox insertion acts as the concurrency boundary. If multiple consumer threads attempt to process the same message, only one transaction can successfully register it.

---

## Outbox Pattern

When a business operation requires an event to be published, the service does not publish directly to Kafka from inside the business transaction.

Instead, the service persists:

* the business state changes
* an Outbox record containing the event payload and metadata

Both are stored in the same local database transaction.

This guarantees that either:

* both the business change and the event record are committed, or
* neither is committed

A background publisher periodically reads pending Outbox records and attempts to publish them to Kafka.

After successful publication, the corresponding Outbox record is marked as published.

The Outbox pattern provides:

* protection against message loss
* elimination of database-and-Kafka dual writes
* recovery from temporary Kafka outages
* asynchronous retry of failed publication
* traceability of pending and published events

A typical producer flow follows this sequence:

```text
Business operation begins
        ↓
Update local business state
        ↓
Store corresponding Outbox event
        ↓
Commit local transaction
        ↓
Background publisher reads pending event
        ↓
Publish event to Kafka
        ↓
Mark Outbox event as published
```

If Kafka is unavailable, the business transaction can still complete locally. The Outbox record remains pending and can be published after connectivity is restored.

---

## Combined Processing Model

For services that both consume and produce events, Inbox registration, business state changes, and Outbox creation occur in one local database transaction.

```text
Incoming Kafka event
        ↓
Register message in Inbox
        ↓
Apply local business changes
        ↓
Create outgoing Outbox event
        ↓
Commit one database transaction
        ↓
Acknowledge consumed Kafka record
        ↓
Publish outgoing event asynchronously
```

This creates a reliable processing chain:

1. the Inbox prevents the incoming event from being applied more than once
2. the local transaction keeps the Inbox, business data, and Outbox state consistent
3. the Outbox ensures that the resulting event is eventually published
4. downstream consumers apply the same Inbox protection again

Each service remains responsible only for its own local database transaction. No distributed transaction is required across Kafka and multiple service databases.

---

## Message Identity

Every integration event contains a unique message identifier.

This identifier represents one concrete event instance and is used for Inbox deduplication.

A retried or redelivered event must retain the same message identifier. A newly created event, even when related to the same order or workflow, receives a new identifier.

Business identifiers such as `orderId` or `paymentId` are not sufficient for technical message deduplication because multiple valid events may exist for the same business entity.

Correlation identifiers are used separately to trace related events across the distributed workflow.

---

## Transaction Boundaries

Inbox registration, local business changes, and Outbox creation must be committed atomically.

The Kafka offset must not be acknowledged before this transaction completes successfully.

If processing fails before the transaction commits:

* the Inbox record is not persisted
* business changes are rolled back
* the Outbox event is not created
* Kafka can redeliver the original message

If processing succeeds:

* the Inbox record prevents future duplicate execution
* business state remains consistent
* any required outgoing event is durably stored in the Outbox

External provider calls are not automatically protected by the Inbox and Outbox patterns. Operations that create external side effects may require additional safeguards, such as provider-supported idempotency keys and explicit business state management.

---

## Delivery Semantics

The architecture does not claim true end-to-end exactly-once delivery.

Kafka messages and Outbox events may be delivered more than once. The system instead provides at-least-once delivery combined with idempotent processing.

The intended result is effectively-once business processing:

* transport-level duplication is allowed
* duplicate business effects are prevented
* outgoing events remain recoverable
* local service state remains transactionally consistent

This distinction avoids relying on stronger delivery guarantees than the underlying distributed components can provide.

---

## Failure Handling

### Duplicate Kafka Delivery

The Inbox detects the existing message identifier and prevents repeated business processing.

### Service Failure Before Transaction Commit

The local transaction is rolled back. Kafka can redeliver the message and processing can start again safely.

### Service Failure After Transaction Commit

The Inbox and business changes remain persisted.

If an Outbox event was created, it remains available for publication after the service restarts.

A redelivered input message is detected as a duplicate through the Inbox.

### Kafka Unavailable During Publication

The Outbox record remains pending.

The background publisher retries publication after Kafka becomes available again.

### Failure After Kafka Publication

A service may publish an event successfully but stop before marking the Outbox record as published.

The event may therefore be published again after restart.

Consumers remain safe because they use Inbox-based deduplication.

### Concurrent Consumer Processing

The unique Inbox message identifier ensures that only one transaction can register and process a particular event.

### Permanent Consumer Failure

After configured retries are exhausted, the failed message is routed to dead-letter handling for investigation and controlled recovery.

---

## Graceful Shutdown

During planned termination, services stop accepting new work while allowing active processing to finish within a bounded period.

New Outbox publication cycles are not started after shutdown begins. Kafka listener containers stop delivering additional records while allowing the currently processed record to complete.

Spring-managed Kafka, database, cache, and external-client resources are closed after active work has been given time to finish.

The Kubernetes pod termination grace period is configured to exceed the application's shutdown timeout.

Graceful shutdown reduces the likelihood of interrupted processing. However, correctness does not depend on graceful shutdown succeeding. If a service is terminated unexpectedly, Inbox deduplication, Outbox persistence, Kafka redelivery, and local transactions provide the recovery mechanism.

---

## Consequences

### Benefits

* duplicate Kafka deliveries can be processed safely
* business changes and outgoing events remain transactionally consistent
* temporary Kafka outages do not cause event loss
* services can recover after crashes or restarts
* no distributed transaction between databases and Kafka is required
* the same reliability model is applied consistently across services
* message processing remains traceable through Inbox and Outbox records

### Trade-offs

* every participating service requires Inbox and Outbox persistence
* additional database writes are required for every processed and produced event
* Outbox publication is asynchronous and introduces eventual consistency
* Outbox and Inbox tables require retention and cleanup strategies
* duplicate event publication remains possible
* consumers must remain idempotent
* monitoring is required for unpublished Outbox events and permanently failing messages

---

## Outcome

The system uses a shared event-processing reliability model across all services.

Incoming events are protected by Inbox-based deduplication. Business state changes and outgoing events are stored atomically using the Outbox pattern. Pending events are published asynchronously and retried when infrastructure is temporarily unavailable.

This design provides reliable at-least-once messaging with effectively-once business processing while preserving service autonomy and avoiding distributed transactions.

