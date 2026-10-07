---
id: dead-letter-investigation
title: Investigate a dead-lettered business message
version: 1
documentType: runbook
services: [ORDER_SERVICE, INVENTORY_SERVICE, PAYMENT_SERVICE, NOTIFICATION_SERVICE, INVESTIGATION_SERVICE]
statuses: []
reasonCodes: []
decisionCodes: []
effective: true
---

# Investigate a dead-lettered business message

A business message reaches dead-letter handling after bounded processing
retries are exhausted. Its presence shows that processing failed repeatedly;
it does not by itself identify the root cause.

Inspect the originating topic, partition, offset, consumer service, exception,
message contract, and related correlation identifiers. Determine whether the
failure is caused by invalid data, incompatible contracts, unavailable local
dependencies, or a recoverable operational condition.

Correct the underlying cause and verify idempotency and current domain state
before controlled replay. Do not automatically route a dead-letter record back
to the same failing path, because a poison message can create an endless
failure cycle.
