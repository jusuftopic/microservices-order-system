# ADR 0006: Single PostgreSQL Instance with Database-per-Service

## Status
Accepted

## Context

The system is designed as a microservice-based, event-driven architecture, where each service owns its data and persists it independently using PostgreSQL.

During early development and initial cloud adoption, the following constraints apply:

- the project is a **side project**
- infrastructure costs must remain within the **AWS free-tier**
- operational simplicity is prioritized
- the system is expected to evolve over time

The ideal microservice setup would use:

- one PostgreSQL instance **per service**
- full physical isolation of data
- independent lifecycle and scaling

However, Amazon RDS free-tier limitations allow only **one always-on database instance** per account. Running multiple PostgreSQL instances would immediately exceed free-tier limits and introduce unnecessary cost for the current project phase.

At the same time, the architecture must:

- preserve **service-level data ownership**
- avoid tight coupling between services
- support a **clean migration path** toward full isolation later

Several options were considered:

- one PostgreSQL instance per service (RDS)
- PostgreSQL deployed inside Kubernetes
- one shared PostgreSQL instance with logical separation
- shared schemas inside a single database

## Decision

The system will use **a single PostgreSQL instance hosting separate databases per microservice**.

Each microservice will:

- connect to its **own dedicated PostgreSQL database**
- use **separate database credentials**
- have no access to other service databases

The PostgreSQL instance will be treated as a shared infrastructure component, while **data ownership remains strictly per service at the logical level**.

This decision applies to development and early cloud stages and may be revisited as the system grows or cost constraints change.

## Rationale

This approach provides a pragmatic balance between architectural correctness and practical constraints.

### Cost Efficiency
- a single PostgreSQL instance fits within the AWS free-tier
- avoids multiplying infrastructure cost per service
- supports long-running environments without manual start/stop cycles

### Architectural Integrity
- each service owns its data via a dedicated database
- no schema sharing or cross-service queries are allowed
- service boundaries are preserved at the data level

### Operational Simplicity
- fewer infrastructure components to manage
- simpler backup, monitoring, and maintenance
- reduced operational overhead for a side project

### Evolutionary Design
- splitting databases into separate PostgreSQL instances later is **simpler than merging data**
- logical separation today enables physical separation tomorrow
- migration can be performed incrementally, service by service

## Consequences

### Benefits
- remains within free-tier cost limits
- clear data ownership per service
- minimal operational complexity
- clean future migration path
- architecture remains production-aligned in intent

### Trade-offs
- shared PostgreSQL instance introduces a larger blast radius
- database upgrades affect all services simultaneously
- resource contention is possible under high load

These trade-offs are accepted given the current project scope, usage expectations, and cost constraints.

## Outcome

The system will:

- maintain strict data ownership per microservice
- operate within AWS free-tier limits
- remain simple to operate and evolve
- support future transition to database-per-service deployment without architectural refactoring

This decision enables pragmatic progress while keeping the architecture aligned with long-term microservice principles.
