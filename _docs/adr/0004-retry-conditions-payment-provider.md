# ADR 0004: Resilient Communication with External Payment Providers

## Status

Accepted

## Context

The Payment Service communicates with an external payment provider to authorize and process customer payments. Unlike internal service communication, external providers operate outside the system boundary and cannot be assumed to be continuously available.

Failures may occur due to:

* temporary network interruptions
* increased response latency
* infrastructure outages
* payment provider degradation
* transient server-side failures

Without dedicated resilience mechanisms, these failures could significantly impact the order processing workflow by increasing request latency, exhausting application resources, or repeatedly sending requests to an already unavailable provider.

The architecture therefore requires a strategy that:

* tolerates transient failures
* prevents cascading failures
* protects the payment provider from unnecessary traffic during outages
* maintains business consistency
* avoids duplicate payment execution

---

## Decision

The Payment Service combines **Retry**, **Circuit Breaker**, and **Idempotency** to provide resilient communication with external payment providers.

Each mechanism addresses a different category of failures while collectively protecting both the application and the external dependency.

---

## Retry Policy

Retries are performed only for failures that are considered temporary and likely to succeed after a short delay.

### Retryable failures

Retries are initiated for transient infrastructure failures, including:

* connection failures
* socket timeouts
* temporary network interruptions
* HTTP 5xx responses
* temporary resource access failures

These failures are considered recoverable and therefore suitable for automatic retry.

### Non-retryable failures

Retries are **not** performed for deterministic business failures, including:

* HTTP 4xx responses
* validation failures
* authentication failures
* authorization failures
* insufficient funds
* rejected payments
* malformed requests

Repeating these requests would not change the outcome and would only increase provider load.

---

## Circuit Breaker

The Circuit Breaker protects both the application and the external payment provider during prolonged outages.

When the configured failure threshold is exceeded, the Circuit Breaker transitions to the **OPEN** state and immediately rejects new payment requests without contacting the provider.

After the configured waiting period, the Circuit Breaker transitions to the **HALF_OPEN** state and allows a limited number of test requests.

If these requests succeed, the Circuit Breaker returns to the **CLOSED** state and normal communication resumes.

If failures continue, the Circuit Breaker immediately returns to the **OPEN** state.

This prevents repeatedly calling an already degraded dependency and significantly reduces the likelihood of cascading failures.

---

## Retry and Circuit Breaker Interaction

The architecture intentionally counts **every physical call to the payment provider** as a Circuit Breaker observation.

Consequently, retry attempts are also recorded by the Circuit Breaker.

For example, a payment configured with three attempts produces the following sequence:

```text
Payment Request
        │
        ▼
Attempt #1  ❌
        │
        ▼
Attempt #2  ❌
        │
        ▼
Attempt #3  ❌
```

From the Circuit Breaker's perspective, this represents **three failed provider calls**, not one failed business operation.

This behaviour is intentional.

The Circuit Breaker measures the health of the **external dependency**, not the success rate of business operations. Every retry represents another request sent to the payment provider and therefore contributes to the observed provider failure rate.

This allows the Circuit Breaker to react more quickly during provider outages and prevents additional unnecessary traffic to an already degraded dependency.

---

## Transaction Boundary

Communication with the external payment provider is intentionally executed **outside the database transaction**.

The Payment Service first commits its local business state before invoking the external provider.

Once the provider responds, the payment state is updated within a separate local transaction.

Separating external communication from database transactions:

* prevents long-running transactions
* reduces database lock contention
* improves scalability
* limits the impact of slow or unavailable external systems

---

## Idempotency

Every payment request includes an idempotency key.

This guarantees that retries or duplicate requests cannot result in multiple charges for the same order.

Idempotency is therefore a prerequisite for safely applying automatic retries.

---

## Observability

Retry and Circuit Breaker behaviour are treated as operational signals rather than hidden implementation details.

The system records:

* retry attempts
* exhausted retry attempts
* Circuit Breaker state transitions
* rejected requests while the Circuit Breaker is OPEN

These events are exposed through structured logs and operational metrics.

Monitoring these signals allows engineering teams to quickly detect payment provider degradation, understand how resilience mechanisms are behaving, and determine whether failures originate from the application or from the external dependency.

---

## Consequences

### Positive

* Temporary provider failures are recovered automatically through retries.
* The Circuit Breaker prevents cascading failures during prolonged outages.
* Database transactions remain independent of external provider response times.
* Idempotency guarantees safe retries without duplicate payment execution.
* Operational teams gain visibility into provider health through logs and metrics.

### Trade-offs

* A single business payment operation may generate multiple physical provider calls due to retries.
* Retry attempts contribute to the Circuit Breaker's failure statistics.
* Temporary provider outages may delay payment completion before fallback handling is triggered.
* The resilience strategy introduces additional operational complexity compared to a simple synchronous HTTP call.
