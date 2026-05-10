# ADR 0003: Kafka & Kubernetes Deployment

## Status

Accepted

------------------------------------------------------------------------

## Context

The system is an event-driven microservice architecture consisting of
services such as order and payment processing, with Kafka used as the
central event backbone for asynchronous communication.

The infrastructure is deployed on Kubernetes. A key architectural decision is whether
Kafka should be deployed:

-   inside the Kubernetes cluster alongside application services, or
-   externally on dedicated infrastructure (e.g. VMs or managed Kafka
    service)

This decision impacts:

-   operational complexity and ownership boundaries
-   system reliability and failure domains
-   infrastructure consistency across environments
-   performance predictability and resource isolation
-   long-term scalability and evolution strategy

Kafka is considered a core infrastructure component of the system,
responsible for durable event streaming between services.

------------------------------------------------------------------------

## Decision

Kafka will be deployed **inside the Kubernetes cluster** as part of the
core platform infrastructure, alongside microservices.

------------------------------------------------------------------------

## Rationale

### 1. Unified platform architecture

Running Kafka within Kubernetes aligns all system components under a
single deployment and operational model, reducing infrastructure
fragmentation.

This enables: 
- consistent deployment patterns across services
- simplified environment parity (local → cloud)
- reduced operational overhead from managing separate infrastructure
  stacks

### 2. Controlled operational scope

Avoids unnecessary separation of infrastructure layers that would
increase operational complexity and observability fragmentation.

### 3. Event backbone as first-class system component

Kafka is treated as a foundational event backbone, ensuring low-latency
and consistent internal communication.

### 4. Platform engineering focus

Enables realistic experience with stateful workloads in Kubernetes.

------------------------------------------------------------------------

## Trade-offs

### Shared failure domain

Cluster failure impacts both Kafka and services.

### Stateful complexity

Requires careful handling of: 
- persistence 
- broker identity 
- rebalancing
- resource contention

### Coupled lifecycle

Kafka lifecycle is tied to Kubernetes cluster lifecycle.

------------------------------------------------------------------------

## Mitigations

-   replication and persistent volumes
-   resource limits and requests
-   monitoring (lag, broker health)
-   retention and backup strategy

------------------------------------------------------------------------

## Alternatives Considered

### External Kafka (VM/bare metal)

Better isolation but higher operational overhead.

### Managed Kafka (e.g. AWS MSK)

Operationally simpler but reduces control and flexibility.

------------------------------------------------------------------------

## Outcome

Kafka is deployed within Kubernetes as a core platform component,
accepting shared failure domains in exchange for operational simplicity
and architectural consistency.
