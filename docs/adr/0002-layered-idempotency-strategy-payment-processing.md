# ADR 0002: End-to-End Payment Processing Safety Model

## Status
Accepted

---

## Context

The system processes payments asynchronously using Kafka.

The Ordering Service publishes `PaymentRequestedEvent`, which is consumed by the Payment Service. The service then executes a payment via an external provider and persists the result.

Because this is a distributed system, we must assume failures will happen:

- Kafka may deliver the same message more than once (at-least-once delivery)
- The service may crash at any point during processing
- The external payment provider may:
    - succeed but timeout
    - fail after processing
    - receive duplicate requests due to retries

These conditions can lead to duplicate payments and inconsistent state if not properly handled.

---

## Decision

We implement a single end-to-end processing flow that ensures correctness through three coordinated mechanisms:

---

### 1. Inbox (Message Deduplication)

Each Kafka event is stored in an `inbox_event` table using an atomic insert.

If the event already exists, it is ignored.

Purpose:
- Prevent duplicate processing of the same Kafka message
- Handle Kafka redelivery safely

---

### 2. Payment State (Business Consistency)

Each order has a `Payment` record with states:

- PENDING
- PROCESSING
- SUCCESS
- FAILED

Rules:
- If SUCCESS → skip processing
- If PROCESSING → skip processing
- Otherwise → set PROCESSING and continue

Purpose:
- Ensure safe retries after crashes
- Prevent duplicate business execution
- Maintain consistent payment lifecycle

---

### 3. External Idempotency Key (Side-Effect Safety)

Each request to the payment provider includes the Kafka `eventId` as an idempotency key.

Purpose:
- Prevent duplicate charges at the external system
- Ensure safe retries after crash or timeout
- Guarantee same result for repeated requests

---

## End-to-End Flow

1. Kafka event is received
2. Inbox is checked (duplicate events are ignored)
3. Payment is loaded or created
4. Payment state is evaluated
5. State is set to PROCESSING
6. External payment provider is called with idempotency key
7. Result is persisted as SUCCESS or FAILED

---

## Failure Handling

- Duplicate Kafka event → handled by Inbox
- Crash after external call → handled by Payment State + idempotency key
- Concurrent processing → prevented by Inbox + state checks
- External retry or timeout → handled by idempotency key

---

## Outcome

The system guarantees that each payment is executed exactly once in effect, even under:
- retries
- crashes
- duplicate Kafka delivery
- external system uncertainty

This ensures financial correctness in a distributed environment.

