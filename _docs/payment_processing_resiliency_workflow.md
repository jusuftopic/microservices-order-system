# Payment Processing Resiliency Workflow

## Goal

This document describes the resiliency pattern used when interacting with the **Payment Provider Client**.

The goal is to ensure:
- reliable communication with external payment systems
- protection against transient failures
- graceful degradation when the provider is unavailable

---

## Key Assumption

> **The PaymentClient MUST have proper timeout configuration**

The resiliency setup assumes:

Without these timeouts:
- requests may hang indefinitely
- retries and circuit breaker cannot react properly
- system threads may get exhausted

---

##  High-Level Workflow


1. **Event Trigger**
    - A payment processing event is emitted after a successful database transaction commit.
    - The event is handled asynchronously to avoid blocking the main application flow.

2. **Payment Invocation**
    - The system invokes the Payment Provider through a dedicated wrapper layer.
    - This layer encapsulates all resiliency mechanisms.

3. **Retry Handling**
    - Transient failures (e.g. network glitches, temporary outages) are automatically retried.
    - Retry attempts are limited and occur with a configured delay.

4. **Circuit Breaker Protection**
    - The system continuously monitors failure rates.
    - If failures exceed a defined threshold:
        - the circuit opens
        - further calls to the payment provider are prevented
    - This avoids overloading an already failing external system.

5. **Timeout Enforcement (Client-Side)**
    - The PaymentClient enforces strict timeouts for all outgoing requests.
    - Slow or unresponsive calls fail fast and are treated as errors.

6. **Fallback Execution**
    - If retries are exhausted or the circuit is open:
        - a fallback response is returned
        - the system degrades gracefully without blocking processing

7. **Finalization**
    - The payment result (successful or fallback) is persisted.
    - The system maintains consistency regardless of external service availability.
