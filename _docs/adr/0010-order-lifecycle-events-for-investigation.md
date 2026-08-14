# ADR 0010: Publish Order Lifecycle Evidence for Investigation

## Status
Accepted

## Context

The Order Service is the saga orchestrator and the authority for the overall
order lifecycle. Existing commands and outcome events execute the workflow,
but an operational consumer would otherwise need to reconstruct why the Order
Service changed an order by joining implementation-specific logs and service
data.

The planned Investigation Service needs a reliable, order-centric history that
can support grounded LLM explanations, validate generated responses against
the available evidence and provide deterministic explanations as fallback.
It must not become part of the critical order workflow or infer authoritative
state from logs.

## Decision

The Order Service will publish a versioned `OrderLifecycleTransitioned` fact on
`order.lifecycle.v1` after every valid committed status transition.

The lifecycle event is persisted through the same transactional outbox as the
order state change. It contains:

- previous and new authoritative status
- a stable reason code
- the service and event that caused the transition
- correlation and causation identifiers
- the next orchestration decision and command identifier, when applicable
- whether compensating work is required and its type
- event occurrence time and contract version

An orchestration decision records intent only. Completion must be established
from an outcome event published by the service that owns the capability.

The event excludes customer, payment and other unnecessary sensitive data. A
future Investigation Service can join lifecycle facts with capability-owned
outcome events by `orderId`, `correlationId`, `causationId` and `commandId`.

## Alternatives

### Reconstruct the lifecycle only from logs

Rejected because logs are operational records rather than stable business
contracts, and their format and retention can change independently.

### Let the Investigation Service query the Order Service database

Rejected because it violates service data ownership and exposes only current
state rather than the reason and causation behind transitions.

### Republish every downstream detail from the Order Service

Rejected because the orchestrator does not own the internal facts of Payment,
Inventory or Notification services. Those services remain responsible for
publishing their own relevant outcomes.

## Consequences

- Investigation remains asynchronous and cannot block order processing.
- Lifecycle evidence is reliable because state and outbox records share a
  local transaction.
- Consumers must handle duplicate delivery and possible out-of-order arrival.
- Contract evolution requires versioning and backward compatibility.
- The lifecycle topic provides business-level evidence; deeper technical root
  cause still requires capability-owned events or observability data.
- LLM-generated explanations must be validated against collected evidence and
  the response contract; failed generation or validation uses a deterministic
  fallback.

## Outcome

The system gains a stable source of lifecycle evidence without transferring
order ownership to the Investigation Service or placing an LLM in the critical
business path.
