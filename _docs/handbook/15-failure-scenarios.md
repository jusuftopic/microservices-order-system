# Failure Scenarios

Distributed systems must assume that individual services, databases, message brokers, and external providers can fail independently.

The architecture therefore focuses on preventing local failures from becoming system-wide failures and on preserving enough state to continue or recover the business workflow.

## Payment Provider Unavailable

If the external payment provider becomes temporarily unavailable, the Payment Service retries only transient failures.

Repeated failed calls contribute to the Circuit Breaker's failure rate. Once the configured threshold is reached, the Circuit Breaker opens and further calls are rejected without contacting the provider.

Idempotency prevents repeated attempts from charging the same order more than once, while metrics and structured logs expose retry activity and Circuit Breaker state changes.

## Kafka Temporarily Unavailable

Business state and outgoing events are stored together in the local database through the Outbox Pattern.

If Kafka is unavailable, the business transaction can still complete locally. The unpublished Outbox record remains stored and can be published once Kafka becomes available again.

This prevents temporary broker outages from causing message loss.

## Consumer Processing Failure

If a consumer cannot process a message, the operation is retried according to the configured policy.

After retries are exhausted, the failed message is routed to dead-letter handling for later investigation instead of being silently discarded.

Inbox records protect consumers against duplicate processing when messages are redelivered.

## Service or Pod Failure

Kubernetes continuously evaluates startup, readiness, and liveness probes.

An unready service is removed from traffic, while an unhealthy container can be restarted. Because services persist important state in their own databases and communicate asynchronously, the failure of one pod does not require the complete business workflow to restart from the beginning.

For planned termination, such as a rolling deployment or scale-down operation, services use graceful shutdown. The terminating instance stops accepting new HTTP requests, stops receiving additional Kafka records, and prevents new scheduled Outbox publication cycles from starting.

Operations already in progress are given time to complete. This includes active database transactions, the currently processed Kafka record, and scheduled work that has already started. Once active processing finishes, Spring closes managed connections to databases, Kafka, and cache infrastructure.

The Kubernetes pod termination grace period is configured to exceed the application shutdown timeout. This provides additional time for final cleanup and normal JVM termination before Kubernetes is permitted to forcefully stop the container.

If the process still terminates before an operation completes, persisted Outbox records, Kafka redelivery, and Inbox-based idempotency allow the workflow to continue safely after another instance starts.

## Database Unavailable

A service that cannot access its database is marked as not ready and should not receive new traffic.

Operations that require local persistence cannot continue until the database becomes available because the database is the authoritative source for the service's business state.

Database failures remain isolated to the owning service rather than directly affecting another service's persistence layer.

## Partial Business Workflow Failure

A distributed order workflow may complete successfully in one service and fail in another.

For example, inventory may be reserved before payment fails. In this case, compensation events are used to release the reservation and move the order into the appropriate terminal state.

This provides eventual consistency without requiring a distributed database transaction.

## Long-Lived Orders and Timeout Handling

Orders that remain in an intermediate state for an extended period (for example, awaiting payment or external confirmation) are handled by a scheduled background process.

A scheduler periodically scans for orders that have exceeded their allowed processing time and transitions them into a TIMEOUT state. This prevents orders from remaining indefinitely open due to missed events, partial failures, or external system issues.

Once marked as TIMEOUT, compensation actions can be triggered, such as releasing reserved inventory or notifying downstream systems. This ensures that stalled workflows are eventually resolved and system resources are not indefinitely locked.

## Notification Failure

A notification failure must not reverse an otherwise successfully completed order.

Notification processing is retried independently, allowing the core business workflow to remain completed while customer communication recovers separately.

## Operational Visibility

Failures are made visible through:

* business and technical metrics
* structured logs with business identifiers
* retry and Circuit Breaker events
* dead-letter records and tables
* health indicators
* Grafana dashboards

This allows  to identify the affected workflow, distinguish temporary degradation from permanent business failure, and determine whether manual intervention is required.
