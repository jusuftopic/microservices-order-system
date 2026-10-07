---
id: missing-downstream-outcome
title: Expected downstream outcome is missing
version: 1
documentType: known-failure-scenario
services: [ORDER_SERVICE, INVENTORY_SERVICE, PAYMENT_SERVICE]
statuses: [INVENTORY_RESERVE_COMPLETED, PAYMENT_COMPLETED, PAYMENT_FAILED]
reasonCodes: []
decisionCodes: [PROCESS_PAYMENT, COMMIT_INVENTORY, RELEASE_INVENTORY, REFUND_PAYMENT]
effective: true
---

# Expected downstream outcome is missing

This scenario applies when lifecycle evidence contains an orchestration
decision but no corresponding completion or failure evidence has arrived.

Possible explanations include delayed processing, consumer lag, exhausted
retries, dead-letter routing, a temporarily unavailable service, an external
provider still processing, or an event that has not yet been published. These
are hypotheses and must not be reported as the confirmed cause.

Start with the decision target service. Use the correlation and command
identifiers from the API timeline to inspect logs and messaging state. Check
consumer lag and the service-owned dead-letter topic. For provider operations,
also inspect the locally persisted provider status and authenticated callbacks.

Do not manually replay a command until its idempotency and current domain state
have been checked.
