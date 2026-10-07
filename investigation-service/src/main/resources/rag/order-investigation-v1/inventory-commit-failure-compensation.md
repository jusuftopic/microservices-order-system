---
id: inventory-commit-failure-compensation
title: Compensation after inventory commit failure
version: 1
documentType: compensation
services: [ORDER_SERVICE, INVENTORY_SERVICE, PAYMENT_SERVICE]
statuses: [PAYMENT_COMPLETED, INVENTORY_COMMIT_FAILED, FAILED]
reasonCodes: [INVENTORY_COMMIT_FAILED, PAYMENT_REFUND_REQUIRED]
decisionCodes: [REFUND_PAYMENT]
effective: true
---

# Compensation after inventory commit failure

If inventory commit fails after payment completed, the customer may already
have been charged. The Order Service records the inventory commit failure,
moves the order to `FAILED`, and selects `REFUND_PAYMENT` with compensation type
`PAYMENT_REFUND`.

The refund decision means that a refund was requested. It does not prove that
the provider accepted or completed the refund. The Payment Service owns refund
processing and the provider outcome.

If the refund outcome is unknown, do not describe it as succeeded or failed.
Inspect the Payment Service refund state, provider interaction, webhook flow,
and reconciliation information when available.
