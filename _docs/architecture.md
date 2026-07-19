# Architecture Overview

## System Flow

Order Service → Outbox → Kafka → Inventory Service (reservation)
Inventory Service -> Inbox -> Processing -> Outbox -> Kafka -> Order Service
Order Service → Inbox -> Processing -> Outbox → Kafka → Payment Service
Payment Service -> Inbox -> Processing -> Outbox -> Kafka -> Order Service
Order Service → Inbox -> Processing -> Outbox → Kafka → Inventory Service (Reservation Commit)
Inventory Service -> Inbox -> Processing -> Outbox -> Kafka -> Order Service
Order Service → Inbox -> Processing -> Outbox → Kafka → Notification Service

## Guarantees

- Outbox ensures reliable event publishing
- Inbox ensures idempotent processing
- Kafka guarantees at-least-once delivery is handled safely in application layer

## Service Boundaries

### Order Service
- Owns order + outbox tables + inbox
- Publishes events only via scheduler
- Act as an orchestrator in Saga Pattern
- Update Ordering Status
- Take Care of the Compensation Steps
- Timeout all long living processing which are not in final state and set final status for them

### Payment Service
- Consumes Kafka events
- Ensures idempotency via inbox table
- Reliable contacting Payment Provider 
- Sends Response message to Order Service via Kafka regarding the payment result

### Inventory Service
- Consumes Kafka events
- Ensures idempotency via inbox table
- React on message to reserve inventory before payment
- Sends Response message to Order Service via Kafka regarding the payment result
- React on message to commit reserved inventory after payment
- Sends Response message to Order Service via Kafka regarding the payment result

### Notification Service
- Consumes Kafka events
- Send message to message provider