# Architecture Overview

## System Flow

Order Service → Outbox → Kafka → Payment Service → Inbox → Processing

## Guarantees

- Outbox ensures reliable event publishing
- Inbox ensures idempotent processing
- Kafka guarantees at-least-once delivery is handled safely in application layer

## Service Boundaries

### Order Service
- Owns order + outbox tables
- Publishes events only via scheduler

### Payment Service
- Consumes Kafka events
- Ensures idempotency via inbox table