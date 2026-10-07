---
id: evidence-interpretation
title: Interpret lifecycle evidence correctly
version: 1
documentType: domain-reference
services: [ORDER_SERVICE, INVESTIGATION_SERVICE]
statuses: []
reasonCodes: []
decisionCodes: [PROCESS_PAYMENT, COMMIT_INVENTORY, RELEASE_INVENTORY, SEND_NOTIFICATION, REFUND_PAYMENT]
effective: true
---

# Interpret lifecycle evidence correctly

An order status transition is an authoritative fact committed by the Order
Service. Its source event explains what caused the transition.

An orchestration decision records the next action selected by the Order
Service. It proves intent, not completion. For example, `RELEASE_INVENTORY`
means that inventory release was requested. Only a later inventory release
completion event proves that the reservation was released.

The same rule applies to payment, inventory commit, refund, and notification
decisions. Never describe an intended downstream action as completed unless a
corresponding outcome is present in the lifecycle evidence.

Missing evidence means that an outcome is not confirmed. It does not prove why
the outcome is missing and must not be turned into a root-cause claim.
