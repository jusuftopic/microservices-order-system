# ADR 0007: Introduce messageId for Inbox Pattern

## Status
Accepted

## Context

The system follows an event-driven, microservice-based architecture using Kafka for communication.
To ensure reliable processing of events, the Inbox pattern is used to achieve idempotent message handling.
Currently, the Inbox implementation uses correlationId to prevent duplicate processing of incoming events.
At the same time, correlationId is used to trace the lifecycle of a workflow (Saga), where a single business process spans multiple services:

- Order creation
- Inventory processing (success / failure)
- Payment processing
- further workflow steps

This means that multiple distinct events within the same workflow share the same correlationId.
As a result, using correlationId for deduplication leads to incorrect behavior:

- the first event is processed successfully
- subsequent valid events are rejected as duplicates

This prevents the correct orchestration of the workflow in the Order Service.
Several options were considered:

- continue using correlationId for Inbox deduplication
- use a composite key (correlationId + eventType)
- introduce a dedicated identifier per message

## Decision

The system will introduce a messageId field for all events.
Each event will:

- include a unique messageId (UUID) generated at creation time
- continue to carry correlationId for workflow tracking

The Inbox pattern will:

- use messageId as the unique key for deduplication
- no longer rely on correlationId

## Rationale
This approach separates two distinct responsibilities:

Workflow Tracking:
- correlationId is used to track the entire lifecycle of a business process
- multiple events belong to the same workflow

Message Idempotency
- messageId uniquely identifies each event
- Inbox deduplication operates at the message level

Using messageId ensures that:
- each event is processed exactly once
- multiple events within the same workflow are handled correctly

Architectural Alignment
- aligns with established event-driven design practices
- reflects how Kafka-based systems handle message identity
- supports orchestrator-based workflows without side effects

## Consequences
Benefits:
- correct idempotent processing of all events
- no accidental rejection of valid messages
- clear separation between tracing and deduplication
- improved observability and debugging

Trade-offs:
- event contracts must be extended with messageId
- Inbox schema must be updated
- producers must generate unique identifiers

These changes are limited in scope and acceptable given the architectural benefits.

Outcome
The system will:
- use messageId for Inbox-based idempotency
- retain correlationId for workflow orchestration
- correctly process multiple events within a single workflow

This decision improves correctness of event handling while maintaining a clean and scalable architecture aligned with long-term system evolution.