---
id: order-lifecycle-happy-path
title: Successful order lifecycle
version: 1
documentType: workflow
services: [ORDER_SERVICE, INVENTORY_SERVICE, PAYMENT_SERVICE, NOTIFICATION_SERVICE]
statuses: [CREATED, INVENTORY_RESERVE_COMPLETED, PAYMENT_COMPLETED, INVENTORY_COMMIT_COMPLETED, COMPLETED]
reasonCodes: [INVENTORY_RESERVED, PAYMENT_COMPLETED, INVENTORY_COMMITTED, ORDER_WORKFLOW_COMPLETED]
decisionCodes: [PROCESS_PAYMENT, COMMIT_INVENTORY, SEND_NOTIFICATION]
effective: true
---

# Successful order lifecycle

The Order Service creates the order and requests inventory reservation. After
the Inventory Service confirms the reservation, the order reaches
`INVENTORY_RESERVE_COMPLETED` and payment processing is requested.

After the Payment Service confirms payment, the order reaches
`PAYMENT_COMPLETED` and inventory commit is requested. After the Inventory
Service confirms the commit, the order progresses through
`INVENTORY_COMMIT_COMPLETED` to `COMPLETED`.

A notification request can be selected after completion. The request does not
prove that the customer notification was delivered.

An intermediate status is not necessarily a failure. It means that the next
downstream outcome has not yet been confirmed in the lifecycle evidence.
