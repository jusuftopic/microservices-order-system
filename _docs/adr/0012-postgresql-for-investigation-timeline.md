# ADR 0012: Persist Investigation Timeline Evidence in PostgreSQL

## Status
Accepted

## Context

The Investigation Service consumes order lifecycle evidence and exposes an
order-centric timeline. The same evidence supports authoritative status
derivation, deterministic explanations, grounded LLM input and validation of
LLM-generated explanations.

Timeline persistence must retain the facts supplied by source events without
making the Investigation Service the owner of the order lifecycle. It must
also support reliable Kafka consumption and reproducible interpretation when
events are redelivered or the investigation projection is rebuilt.

## Required Characteristics

The selected storage must provide:

- idempotent ingestion with database-enforced uniqueness for event identifiers
- immutable, append-oriented retention of lifecycle evidence
- efficient retrieval of all relevant evidence for one `orderId`
- stable ordering using persisted event and messaging metadata
- support for duplicate, late and out-of-order event delivery
- transactional persistence before successful consumer acknowledgement
- preservation of event type and contract version
- evolution from lifecycle transitions to additional capability-owned evidence
- rebuildable derived status and explanation projections
- controlled retention of evidence without unnecessary sensitive data
- straightforward local operation and portability to Kubernetes environments
- mature Java and Spring Boot integration, migrations and observability

The dominant access pattern is a relatively small timeline queried by
`orderId`, rather than analytics across large global time ranges.

## Considered Technologies

### Relational database

A relational database provides strong uniqueness constraints, transactions,
explicit schema evolution and efficient indexed order-centric queries. Its
structured model matches the common evidence envelope, while PostgreSQL also
supports flexible evidence-specific details when event types differ.

### Document-oriented NoSQL database

A document database naturally accommodates evidence with different shapes and
can efficiently query documents by `orderId`. The flexibility does not remove
the need for contract validation, version handling, uniqueness and indexing.
For this workload, it introduces another storage technology without a
demonstrated requirement that PostgreSQL cannot satisfy.

### Event store

An event store provides immutable streams, stream revisions and projection
rebuilding. The Investigation Service does not own the order aggregate,
however, and already receives integration evidence through Kafka. Adding an
event store would duplicate log, replay and subscription responsibilities
without establishing a distinct source-of-truth role.

### Time-series database

A time-series database is optimized for time-window aggregation and analysis
across large volumes. Investigation queries are primarily scoped to a
high-cardinality order identifier and require business-causal interpretation,
not time-series aggregation. Time-series specialization therefore adds little
value to the dominant access pattern.

## Decision

PostgreSQL is used as the Investigation Service persistence technology.

The Investigation Service owns its logical database and credentials. It does
not query or modify databases owned by other services. A shared physical
PostgreSQL installation may be used where appropriate without weakening
logical database ownership.

The complete `OrderLifecycleTransitionedEvent` evidence is retained, including
event identity, order identity, previous and new status, reason, causation,
orchestration decision, compensation information, occurrence time and contract
version. Consumer-side metadata needed for ingestion diagnostics and stable
ordering is retained alongside the business evidence.

PostgreSQL stores both strongly structured common evidence and flexible
evidence-specific details. Flexibility does not replace application-level
validation or explicit contract evolution.

The Investigation Service owns interpretation of the evidence. Authoritative
status, compensation state, evidence completeness and explanations are derived
by application logic. Any persisted derived projection remains rebuildable
from immutable evidence.

## Consequences

- Database uniqueness and transactions provide a strong idempotent-consumer
  boundary.
- Indexed order-centric queries support timeline retrieval without specialized
  storage infrastructure.
- Existing PostgreSQL, Java and Spring Boot knowledge is reused across the
  project.
- The system avoids introducing polyglot persistence without a workload-driven
  reason.
- Schema changes require controlled migrations even when flexible evidence
  fields are used.
- Stable replay depends on complete event history and deterministic application
  logic; PostgreSQL does not determine business order by itself.
- Database backup and Kafka replay remain different recovery mechanisms.
- Operating PostgreSQL as a stateful dependency requires persistence,
  monitoring and recovery practices independently of application deployment.
- A different storage model remains possible if measured workload or query
  requirements no longer fit this decision.

## Outcome

The Investigation Service gains a transactional and queryable evidence store
that supports reliable timeline construction, deterministic fallback and
grounded LLM explanations while preserving service ownership and operational
simplicity.
