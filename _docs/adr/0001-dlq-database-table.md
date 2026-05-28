# ADR 0001: Use Database-based DLQ for Application-Level Failures and Kafka DLQ for Transport-Level Failures

## Status
Accepted

## Context

The system uses the Inbox + Outbox pattern across all services to reliably process and publish domain events via Kafka.

During implementation, we identified two fundamentally different categories of failures:

### 1. Application-Level Failures
Occur before or during event persistence:
- Invalid payload structure
- Serialization issues
- Business logic inconsistencies
- Domain validation failures

These events never reach Kafka and represent invalid or incomplete domain state.

### 2. Transport-Level Failures
Occur after event persistence during message delivery:
- Kafka broker unavailable
- Network issues
- Producer configuration errors
- Temporary infrastructure outages


Without clear separation, these failures can lead to:
- infinite retries on poison messages
- loss of observability
- inability to distinguish domain issues from infrastructure issues
- polluted Kafka topics with invalid message


## Decision

We introduce a **dual DLQ strategy** separating application concerns from transport concerns:

### 1. Database-based DLQ (Application-Level)
Each service maintains a local DLQ table that stores:
- events that could not be created or serialized
- domain-level failures during Outbox creation
- invalid or inconsistent business data

These events:
- never reach Kafka
- are stored locally for inspection and manual recovery
- are not retried automatically


### 2. Kafka Dead Letter Topic (Transport-Level)
Kafka DLQ (DLT) is used exclusively for:
- failures during message delivery
- deserialization errors on consumers
- repeated processing failures after retries

These failures occur after the event has been successfully persisted in the Outbox.


This ensures clear separation of concerns and prevents invalid events from entering the messaging system.

## Consequences

### Positive
- Clear separation between domain failures and transport failures
- Prevents invalid events from reaching Kafka
- Avoids infinite retry loops for poison messages
- Improves observability and debugging capabilities
- Enables targeted replay strategies (DB vs Kafka DLQ)
- Keeps Outbox focused on valid, publishable events


### Negative
- Requires maintaining both DB DLQ and Kafka DLQ
- Introduces additional operational complexity
- Requires clear operational procedures for replay and recovery

## Alternatives considered

### 1. Kafka Dead Letter Topic (DLT) ONLY
Rejected as a single solution because:
- does not distinguish between application and transport failures
- allows invalid domain events to enter Kafka
- complicates debugging of business logic issues

Kafka DLQ is still used, but only for transport-level failures.

### 2. Infinite retry with exponential backoff
Rejected because:
- poison messages would still block system resources
- no clear failure boundary

## Outcome

Each service implements:

- an Inbox for idempotent processing
- an Outbox for reliable event publishing
- a local DLQ table for application-level failures
- Kafka DLQ (DLT) for transport-level failures

This results in a robust, production-grade failure handling strategy that clearly separates domain concerns from infrastructure concerns.