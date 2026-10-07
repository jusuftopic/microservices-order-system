---
id: late-payment-after-timeout
title: Payment completes after order timeout
version: 1
documentType: known-failure-scenario
services: [ORDER_SERVICE, PAYMENT_SERVICE]
statuses: [TIMED_OUT]
reasonCodes: [ORDER_PROCESSING_TIMEOUT, PAYMENT_COMPLETED]
decisionCodes: [REFUND_PAYMENT]
effective: true
---

# Payment completes after order timeout

A payment provider can confirm success after the Order Service has already
moved the order to `TIMED_OUT`. The successful payment is still an important
payment fact, but it must not resume inventory commit or normal fulfillment for
the timed-out order.

The safe response is to preserve the late success and request payment refund
compensation. Repeated observations must not initiate duplicate refunds.

During investigation, verify the timeout transition, the late payment result,
and the refund request independently. A refund request is not proof that the
refund completed.
