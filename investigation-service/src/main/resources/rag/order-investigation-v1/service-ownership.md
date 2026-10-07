---
id: service-ownership
title: Service ownership during investigation
version: 1
documentType: ownership
services: [ORDER_SERVICE, INVENTORY_SERVICE, PAYMENT_SERVICE, NOTIFICATION_SERVICE, INVESTIGATION_SERVICE]
statuses: []
reasonCodes: []
decisionCodes: []
effective: true
---

# Service ownership during investigation

The Order Service owns the overall order lifecycle, status transitions,
workflow decisions, timeout handling, and compensation decisions. Investigate
it when a transition or the next orchestration decision is missing or invalid.

The Inventory Service owns stock, reservation, commit, and release operations.
Investigate it when an inventory command was selected but its inventory outcome
is missing or failed.

The Payment Service owns payment records, provider interaction, payment state,
refund processing, and provider callbacks. Investigate it when a payment or
refund outcome is missing, rejected, processing, or unknown.

The Notification Service owns delivery of customer notifications. Notification
failure must not reverse an otherwise completed order.

The Investigation Service owns its timeline projection and explanation. It
does not own or change the authoritative order, inventory, payment, or
notification state.
