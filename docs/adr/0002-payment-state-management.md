# ADR 0002: Payment Lifecycle State Is Managed Inside Payment Service

## Status
Accepted

## Context

The system uses event-driven communication between `order-service` and `payment-service`.

When an order is created, `order-service` publishes a `PaymentRequested` event.  
The `payment-service` consumes this event and starts payment processing.

A design question emerged:

Should failed payments be retriggered externally (for example by replaying Kafka events), or should retries and state transitions be handled internally by `payment-service`?

This is especially relevant for scenarios such as:

- payment provider timeout
- temporary network failure
- provider outage
- circuit breaker open state
- retryable technical errors
- permanent business rejection (e.g. decline)

## Decision

The `PaymentRequested` event is treated as a **single business trigger**.

After the initial trigger is accepted, all further payment lifecycle management is handled internally by `payment-service`.

This includes:

- payment status transitions
- retries against provider APIs
- retry scheduling
- circuit breaker behavior
- fallback provider logic
- timeout handling
- final success or failure state

The Inbox pattern remains responsible only for external idempotency (preventing duplicate triggers of the same incoming event).

## Responsibilities

### External Messaging Layer

Responsible for:

- delivering `PaymentRequested`
- deduplicating incoming events via Inbox table
- triggering payment process once

### Payment Service Domain Logic

Responsible for:

- `REQUESTED`
- `PROCESSING`
- `SUCCESS`
- `FAILED_RETRYABLE`
- `FAILED_PERMANENT`
- retries
- provider resilience patterns

## Consequences

### Positive

- Prevents duplicate external payment triggers
- Clear separation of technical idempotency vs business workflow
- Cleaner ownership boundaries
- Easier to add retry schedulers, circuit breakers, fallback providers
- Reduces coupling between services
- Avoids replaying Kafka events for business retries

### Negative

- Payment service becomes more stateful
- Requires internal retry orchestration
- More domain logic inside payment module

## Alternatives Considered

### 1. Re-trigger payments via Kafka replay

Rejected because:

- risks duplicate charges
- mixes transport retries with business retries
- weak ownership boundaries
- harder operational control

### 2. Let upstream services decide retries

Rejected because:

- upstream systems do not know provider failure semantics
- creates tight coupling
- spreads payment logic outside payment domain

## Outcome

The payment request event is consumed once.  
Afterward, `payment-service` owns the full payment lifecycle until terminal completion (`SUCCESS` or permanent `FAILED`).