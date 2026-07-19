# Architectural Trade-offs

The architecture prioritizes reliability, service autonomy, and operational clarity. These qualities come with additional complexity that would not be justified for every system.

## Microservices and Independent Data Ownership

Separating the system into independently deployable services allows each domain to evolve, scale, and fail independently. Each service owns its own database, ensuring there is no shared database and preventing hidden coupling between services.

The trade-off is increased operational complexity. Business workflows now cross service and database boundaries, requiring asynchronous communication, eventual consistency, distributed debugging, and explicit failure handling. Managing multiple independent databases also introduces operational overhead. Each service requires its own schema management, migrations, backups, monitoring, and connection handling.

## Event-Driven Communication

Kafka reduces direct runtime coupling between services and allows business processes to continue asynchronously.

However, asynchronous communication makes workflows harder to understand and test. Message duplication, delayed delivery, ordering, schema evolution, and partial failures must be handled explicitly.

## Inbox and Outbox Patterns

Inbox and Outbox patterns improve messaging reliability by coordinating database state with message production and consumption.

This reliability requires additional database tables, scheduled processing, cleanup strategies, duplicate detection, and operational monitoring. The system accepts this complexity because losing or processing critical business events more than once would be more costly.

## Retry and Circuit Breaker Protection

Retries improve recovery from short-lived provider failures, while the Circuit Breaker protects the system during longer outages.

These mechanisms can also increase latency and provider traffic if configured too aggressively. Retry attempts contribute to Circuit Breaker statistics, meaning resilience settings must be tuned according to traffic volume and provider behaviour.

## Shared Messaging Infrastructure

The `messaging-starter` reduces duplication by centralizing common Inbox, Outbox, retry, and dead-letter infrastructure.

The trade-off is compile-time coupling between services and the shared module. Changes to shared infrastructure may require validation and redeployment of several services, so the module must remain technical and avoid accumulating business logic.

## Monorepository and Selective CI

A monorepository simplifies dependency management, architectural testing, development speed and coordinated changes across services. Selective builds preserve faster feedback by validating only affected components.

The pipeline must understand shared dependencies correctly. Changes to common modules or root configuration may affect every service, making change detection more complex than a simple full-repository build, while still saving resources by building only what is affected.

## Kubernetes Security Controls

Internal services communication , NetworkPolicies, isolated Secrets, and hardened containers reduce exposure and limit the impact of compromised workloads.

These controls add configuration and troubleshooting overhead. Incorrect policies can block legitimate traffic, while Kubernetes Secrets and network restrictions still do not replace application-level authentication, encryption, and dedicated secret management.

## Testing Depth

Unit, integration, and architecture tests provide strong confidence in individual services, infrastructure guarantees, and structural boundaries.

They do not fully prove that the complete distributed workflow behaves correctly under production-scale traffic. End-to-end, contract, regression, and load testing would provide broader confidence but would also increase execution time, maintenance cost, and infrastructure requirements.

## Overall Assessment

The system deliberately exchanges simplicity for stronger reliability, isolation, and operational control.

For a smaller application with limited scale, a modular monolith and synchronous transactions could provide sufficient business value with considerably less complexity. The selected architecture becomes most valuable when independent evolution, fault isolation, reliable asynchronous processing, and operational visibility are important requirements.
