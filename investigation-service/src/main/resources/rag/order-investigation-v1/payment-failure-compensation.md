---
id: payment-failure-compensation
title: Compensation after payment failure
version: 1
documentType: compensation
services: [ORDER_SERVICE, PAYMENT_SERVICE, INVENTORY_SERVICE]
statuses: [INVENTORY_RESERVE_COMPLETED, PAYMENT_FAILED, FAILED]
reasonCodes: [PAYMENT_FAILED, COMPENSATION_COMPLETED]
decisionCodes: [RELEASE_INVENTORY]
effective: true
---

# Compensation after payment failure

If payment fails after inventory was reserved, the Order Service records
`PAYMENT_FAILED` and selects `RELEASE_INVENTORY` with compensation type
`INVENTORY_RELEASE`.

The Inventory Service is responsible for releasing the reservation and
reporting the result. Until an inventory release completion event appears, the
release is requested but not confirmed.

After release completion, the Order Service can move the order to `FAILED` and
request the appropriate failure notification. Notification delivery remains a
separate concern and is not proof of compensation completion.

When release confirmation is missing, inspect the inventory command flow,
Inventory Service processing, consumer lag, and dead-letter handling. These are
investigation steps, not confirmed causes.
