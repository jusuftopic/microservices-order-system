# Resilience and Fault Tolerance

## Purpose

Distributed systems operate in an environment where failures are expected rather than exceptional, and this project is intentionally designed with failure as a first-class concern. Network interruptions, temporary infrastructure outages, unavailable external providers, duplicate message delivery, and unhealthy service instances are all situations that must be handled without compromising the integrity of the business workflow.

Previous chapters introduced the mechanisms that ensure reliable message delivery. This chapter focuses on how the system remains operational when individual components become temporarily unavailable or fail unexpectedly.

## Resilience Strategy

The architecture applies several complementary mechanisms to improve operational resilience:

* retry transient failures
* isolate permanently failing operations
* prevent cascading failures
* avoid long-running database transactions
* recover automatically from unhealthy service instances
* preserve business consistency during partial failures

Rather than relying on a single technique, resilience is achieved by combining multiple independent layers of protection.

## Reliable Communication with External Systems

External systems are inherently less predictable than local components and are typically treated as black-box systems, where internal implementation details are unknown. Network latency, temporary outages, and provider-side failures are expected operational conditions.

For this reason, communication with external providers is performed using resilient communication patterns rather than assuming every request succeeds immediately.

### Retry Strategy

Transient failures are handled through automatic retries.

Rather than immediately reporting a failure, the service performs additional attempts before concluding that the operation cannot be completed successfully.

This strategy improves the overall success rate for temporary problems such as:

* short network interruptions
* temporary provider unavailability
* intermittent infrastructure failures

Retries are intentionally limited to prevent excessive load on already degraded systems.

### Circuit Breaker

Repeatedly calling an unavailable external provider wastes resources and increases recovery time.

The architecture therefore applies the Circuit Breaker pattern.

When repeated failures are detected, outgoing requests are temporarily suspended. During this period the provider is allowed to recover without receiving additional traffic.

After the configured waiting period, a limited number of requests are allowed again to determine whether the provider has recovered successfully.

This prevents cascading failures while protecting both the application and the external provider from unnecessary load.

## Transaction Boundaries

One important architectural decision is that communication with external providers is intentionally separated from database transactions.

Business data is committed first.

Only after the local transaction has completed successfully is the external provider invoked.

This avoids holding database locks while waiting for slow or unavailable external systems and significantly reduces the impact of temporary provider outages on the overall application.

The result is shorter database transactions, improved scalability, and lower contention under load.\

### Example: Payment Service and External Payment Provider

The interaction between the Payment Service and the external payment provider demonstrates how resilience mechanisms work together to protect the order processing workflow.

The Payment Service first persists its local payment state and commits the transaction before invoking the external provider. This keeps database transactions short and prevents external latency or outages from blocking the application's local resources.

Communication with the provider is protected by retries for transient infrastructure failures and a Circuit Breaker for prolonged outages. Each retry represents a real provider call and therefore contributes to the Circuit Breaker's failure statistics, allowing it to accurately measure the health of the external dependency. Combined with idempotent payment requests, these mechanisms enable the system to recover from temporary failures while preventing duplicate payment execution.

This approach ensures that:

* database consistency is preserved regardless of external failures
* long-running database transactions are avoided
* temporary failures are recovered automatically whenever possible
* prolonged provider outages are isolated by the Circuit Breaker
* duplicate payment execution is prevented through idempotency

## Notification Reliability

Customer notification is intentionally treated as a supporting capability rather than part of the critical business workflow.

A failure to deliver a notification must never invalidate an otherwise successful order.

If notification delivery cannot be completed immediately, retry mechanisms attempt delivery again. Persistent failures are isolated for later investigation without affecting the completed business transaction.

This separation ensures that customer communication problems never compromise business correctness.

## Internal Availability and Health Management

Resilience also includes detecting failures within the application itself.

Each service continuously exposes its operational health so that the surrounding infrastructure can determine whether the instance is able to perform its responsibilities safely.

Health information includes verification of critical dependencies such as:

* local database connectivity
* Kafka connectivity
* successful application startup
* application readiness to process requests

Database connectivity is monitored through dedicated database health indicators and additional diagnostic logging. Kafka connectivity is verified through a custom health indicator that confirms the service can communicate with the messaging infrastructure.

These health signals provide early visibility into dependency failures before they affect business processing.

## Graceful Shutdown

Resilience applies not only to unexpected failures but also to planned service termination during deployments, scaling operations, and infrastructure maintenance.

When Kubernetes terminates a pod, the application enters a controlled shutdown state instead of stopping immediately. This allows the instance to stop accepting new work while giving already-running operations time to complete safely.

The shutdown process follows these principles:

* the application records that shutdown has started
* new HTTP requests are no longer accepted
* Kafka consumers stop receiving additional records
* scheduled Outbox publication cycles are not started
* currently executing database transactions and in-memory operations are allowed to finish
* Spring-managed resources, including Kafka, database,  cache and 3rd party connections, are closed after active processing completes
* the process exits before Kubernetes reaches its forced termination deadline

The shutdown state is represented internally through a thread-safe lifecycle flag. Components that initiate background work, such as the Outbox scheduler, check this state before starting a new processing cycle.

Kafka listeners are not skipped manually through this flag. Instead, the Kafka listener container controls consumer shutdown. The currently processed record is allowed to finish, while additional records are no longer delivered to the application. This ensures that a message is not acknowledged without completing its business operation.

The application shutdown timeout is intentionally shorter than the Kubernetes pod termination grace period. This creates a safety margin for final resource cleanup, connection closure, log flushing, and JVM termination before Kubernetes is allowed to forcefully stop the container.

The resulting shutdown sequence is:

```text
Kubernetes sends termination signal
        ↓
Application enters shutdown state
        ↓
Readiness is removed and new work is stopped
        ↓
Current HTTP, Kafka, and scheduled work finishes
        ↓
Managed infrastructure connections are closed
        ↓
Application exits normally
        ↓
Kubernetes grace period ends only if shutdown did not complete
```

This mechanism reduces the risk of interrupted transactions, partially processed messages, duplicated work, and abrupt connection termination during rolling deployments or pod scaling.


## Kubernetes Health Probes

The Kubernetes platform continuously evaluates the health of each service instance using three complementary probe types.

### Startup Probe

Startup probes determine whether an application has completed its initialization successfully.

This prevents Kubernetes from restarting services that require additional startup time before becoming operational.

### Readiness Probe

Readiness probes determine whether a service is currently capable of handling requests.

When critical dependencies such as the database or Kafka become unavailable, the service is temporarily removed from traffic until it becomes ready again.

This prevents requests from reaching instances that cannot safely execute business operations.

### Liveness Probe

Liveness probes determine whether the application is still functioning correctly.

If an application becomes unresponsive or enters an unrecoverable state, Kubernetes automatically restarts the affected instance.

This enables automatic recovery without requiring manual operational intervention.

## Layered Fault Tolerance

The architecture combines multiple resilience mechanisms, each protecting a different failure scenario.

| Failure Scenario                  | Protection Mechanism                                |
| --------------------------------- | --------------------------------------------------- |
| Temporary provider outage         | Retry                                               |
| Repeated provider failures        | Circuit Breaker                                     |
| Long-running external calls       | External communication outside database transaction |
| Notification delivery failure     | Retry and isolation                                 |
| Database unavailability           | Health monitoring and readiness checks              |
| Kafka unavailability              | Kafka health monitoring                             |
| Unresponsive application instance | Kubernetes liveness probe                           |
| Slow application startup          | Kubernetes startup probe                            |

No single mechanism guarantees system resilience on its own. Instead, the architecture combines multiple independent protection layers that together minimize service disruption while preserving business consistency.
