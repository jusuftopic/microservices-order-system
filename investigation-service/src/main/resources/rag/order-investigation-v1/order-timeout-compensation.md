---
id: order-timeout-compensation
title: Compensation after order timeout
version: 1
documentType: compensation
services: [ORDER_SERVICE, INVENTORY_SERVICE, PAYMENT_SERVICE]
statuses: [INVENTORY_RESERVE_COMPLETED, PAYMENT_COMPLETED, TIMED_OUT]
reasonCodes: [ORDER_PROCESSING_TIMEOUT]
decisionCodes: [RELEASE_INVENTORY, REFUND_PAYMENT]
effective: true
---

# Compensation after order timeout

The Order Service can move an order that remains in an intermediate state past
its processing deadline to `TIMED_OUT`. The required compensation depends on
the last confirmed state before the timeout.

If inventory was reserved but payment was not confirmed, releasing inventory
is the relevant compensation. If payment was confirmed, both payment refund
and inventory release can be required.

`TIMED_OUT` is a terminal order state. A later result must not silently resume
normal fulfillment. Late outcomes still need to be preserved and handled by an
appropriate compensation or controlled review path.
