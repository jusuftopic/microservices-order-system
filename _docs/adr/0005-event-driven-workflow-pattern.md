# ADR 0005: Orchestrated Saga Pattern for Order Workflow

## Status
Accepted

## Context

The system is evolving from a simple integration between Order Service and Payment Service into a distributed workflow involving multiple services, including Inventory Service and future extensions such as Notification Service.

Previously, the flow consisted of:

- Order Service receives request
- Order is stored
- Event is published to Kafka
- Payment Service processes payment

This approach does not provide:

- feedback from downstream services
- proper order state transitions
- handling of multi-step workflows
- consistency across services

With the introduction of Inventory Service, the workflow now includes multiple steps which must be coordinated:

- inventory validation must happen before payment
- failures must be handled explicitly
- order state must reflect progress across services

Two approaches were considered:

- Choreography-based saga (fully distributed decision making)
- Orchestration-based saga (central workflow control)

## Decision

The system will use an **Orchestrated Saga Pattern**, with the **Order Service acting as the orchestrator of the workflow**.

The Order Service will:

- own and manage the order lifecycle
- initiate each step of the workflow
- react to result events from other services
- decide the next step based on those results

Other services (Inventory, Payment) will:

- process requests
- emit result events
- not contain workflow decision logic

For side effects that do not influence the main workflow (e.g. notifications), a simple **choreography approach** will be used.

## Workflow

The workflow will follow these steps:

1. Order Service receives an order request
2. Order Service stores order data and creates an outbox event
3. Scheduler publishes the outbox event to Kafka

4. Order Service emits an event to request inventory check

5. Inventory Service:
    - consumes event using inbox pattern
    - checks available inventory
    - reserves stock if available
    - emits result event:
        - inventory successful
        - inventory failed

6. Order Service:
    - consumes inventory result event
    - if inventory is successful:
        - updates order status
        - emits event to request payment
    - if inventory fails:
        - updates order status to FAILED

7. Payment Service:
    - consumes event using inbox pattern
    - stores payment request
    - calls external payment provider
    - emits result event:
        - payment successful
        - payment failed

8. Order Service:
    - consumes payment result event
    - updates order status accordingly:
        - COMPLETED if successful
        - FAILED if payment fails

## Failure Handling

- All services use the Inbox pattern for idempotent message consumption
- All services use the Outbox pattern for reliable message publishing
- Kafka retries are used for transient failures
- messages that cannot be processed are moved to a dead-letter topic

Failure scenarios:

- inventory failure → order marked as FAILED
- payment failure → order marked as FAILED
- future: compensation actions (e.g. releasing reserved stock)

## Rationale

The orchestrated approach provides:

- clear control over workflow progression
- centralized state management
- easier debugging and tracing
- explicit handling of failures

Choreography alone was not selected due to:

- distributed and implicit control flow
- increased complexity as the system grows
- difficulty in debugging multi-step workflows

A hybrid approach is used:

- orchestration for core business workflow
- choreography for independent side effects

## Outcome

The system will:

- maintain a consistent order lifecycle
- support multi-step distributed workflows
- be extensible for additional services
- provide clear control over execution flow

The architecture remains flexible and scalable for future enhancements such as notifications, shipping, and additional integrations.