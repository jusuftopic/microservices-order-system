# ADR 0004: Define Retry Conditions for Payment Service Calls

## Status
Accepted

## Context

The payment-service communicates with an external payment provider to charge customer orders. This communication is inherently unreliable due to network variability, service degradation, and transient infrastructure failures.

To improve resilience, we introduced a retry mechanism using a fault tolerance library. However, indiscriminate retries can lead to:

- duplicate payment attempts (if idempotency is not correctly applied)
- increased load on the payment service during outages
- longer request latency for end users
- masking of non-retryable business failures

Therefore, we need to define which failure scenarios are eligible for retry and which are not.

## Decision

Retries will be initiated only for transient, infrastructure-related failures where the likelihood of success increases with subsequent attempts.

### Retryable scenarios

A retry is triggered when the failure is caused by temporary or recoverable conditions, including:

#### 1. Network-level failures
- Connection timeouts
- Socket timeouts
- Connection resets
- DNS resolution failures

#### 2. Infrastructure unavailability
- Payment service temporarily unreachable
- Load balancer or gateway failures (e.g. 502, 503, 504)
- Resource access failures caused by network interruption

#### 3. Temporary server-side errors
- HTTP 5xx responses from payment provider (excluding known business failures)
- Internal server errors not related to validation or business logic

#### 4. Client-side transport exceptions
- I/O exceptions during HTTP communication
- Spring RestClient transport-related exceptions


### Non-retryable scenarios

Retries MUST NOT be initiated for deterministic or business-related failures:

- HTTP 4xx client errors (e.g. validation failure, unauthorized access)
- Insufficient funds or rejected payments
- Invalid request payloads
- Authentication or authorization failures
- Business rule violations returned by payment provider
- Consistently failing “poison” requests

## Outcome

The system implements a strict retry policy limited to transient infrastructure and network failures, combined with idempotent payment requests to ensure correctness under retry scenarios.