# ADR 0015: Propagate Order Ownership as an Independent Event

## Status
Accepted

## Context

The Order Service accepts customer requests and is the authority that creates
orders. When an authenticated customer creates an order, the Order Service can
derive a stable customer identifier from the validated access token and assign
that customer as the resource owner.

Other services may later expose customer-facing APIs for data derived from the
order. The Investigation Service is the first such consumer: it exposes an
order timeline and question endpoints from its local evidence projection. It
must enforce access to a particular order without querying the Order Service
database or trusting an order identifier supplied by the caller.

This requirement is expected to apply to additional services as the system
evolves. The solution must therefore preserve service autonomy and avoid a
synchronous authorization dependency from every API service to the Order
Service.

The existing `OrderLifecycleTransitionedEvent` is an authoritative record of
an order status transition. As established by
[ADR 0010](0010-order-lifecycle-events-for-investigation.md), it deliberately
excludes customer data that is unnecessary for explaining lifecycle changes.
Adding ownership to this event would repeat the same value on every transition
and mix resource-access metadata with lifecycle evidence.

Spring Security's `Authentication` object and the access token are
request-scoped security credentials. They are not durable domain data: tokens
expire, contain claims that may change, and must not be persisted in Outbox
records or Kafka topics.

## Decision

Order ownership is published once as an independent, versioned integration
fact when an order is created and ownership is assigned.

The Order Service will publish an `OrderOwnershipAssignedEvent` through its
transactional Outbox on a dedicated `order.ownership.v1` topic. Creation of the
order, assignment of its owner and persistence of the Outbox record occur in
the same local transaction.

The event contains only the information needed to establish resource access:

- order identifier
- stable, application-controlled customer identifier
- event occurrence time and contract version
- correlation identifier and stable message identifier

The customer identifier is an opaque internal identifier. It is derived from a
validated identity and must not be accepted from the order request body. Email
addresses, usernames, access tokens, refresh tokens, Spring Security objects
and the caller's scope list are excluded from the event.

The identity system is responsible for mapping an authenticated external
identity, represented by issuer and subject, to the internal customer
identifier. Supporting that mapping and account linking is a separate identity
management responsibility. Services use the signed customer identifier claim
from a validated access token; they do not use email as a security identity.

Only services with a concrete customer-facing authorization requirement
consume the ownership event. Each consumer persists a minimal local
authorization projection keyed by `orderId`. It validates an incoming access
token independently and compares its authenticated customer identifier with
the locally projected owner before returning order-specific data.

The lifecycle evidence model remains separate from the authorization
projection. In the Investigation Service, ownership is not duplicated on every
`OrderLifecycleEvidence` row. Resources such as question executions that
already refer to an `orderId` are authorized through that order's local access
projection.

If ownership has not yet reached a consumer because of asynchronous projection
lag or failure, access is denied. A missing ownership projection must never
default to allowing access.

Customer identity in the event is business data and does not authenticate the
producer. Kafka authentication and topic ACLs must independently ensure that
only the Order Service can publish authoritative order-ownership facts.

If ownership reassignment becomes a supported business capability, it requires
an explicit versioned ownership-change event and defined transition rules. It
must not be introduced as a silent update to consumer projections.

## Rationale

Separating ownership from lifecycle transitions gives ownership its own
meaning, retention policy and consumer set. Services that need authorization
can subscribe without forcing Inventory, Payment, Notification or other
workflow participants to receive identity data they do not use.

A local projection lets an API service make authorization decisions without a
runtime dependency on another service. This preserves availability and service
data ownership while accepting the same eventual-consistency model already
used elsewhere in the system.

An internal customer identifier prevents business data from depending on a
particular identity provider. Issuer and subject remain authoritative external
identity coordinates, while account linking can associate more than one
external identity with the same internal customer.

## Alternatives

### Add ownership to `OrderLifecycleTransitionedEvent`

Rejected because ownership would be repeated on every status transition and
would broaden a lifecycle evidence contract with unrelated authorization data.
It would also weaken the data-minimization decision in ADR 0010.

### Add ownership to every workflow command and outcome event

Rejected because most workflow consumers do not need customer identity. It
would duplicate data across contracts and cause producers to know about
consumer-specific authorization needs.

### Query the Order Service for every authorization decision

Rejected because it creates synchronous runtime coupling, adds latency and
makes each dependent API's availability rely on the Order Service. It also
scales poorly as more services expose APIs.

### Read ownership from the Order Service database

Rejected because it violates service-owned data and bypasses the Order
Service's authoritative boundary.

### Propagate the access token or `Authentication` object through Kafka

Rejected because tokens are credentials with finite lifetimes and broader
claims than consumers require. `Authentication` is a Spring runtime object,
not an integration contract. Persisting either would increase leakage and
replay risk without providing durable ownership.

### Use email as the distributed ownership identifier

Rejected because email can change, be reassigned, have inconsistent
normalization and originate from issuers with different verification policies.
It remains contact information rather than a security identity.

## Consequences

- `OrderLifecycleTransitionedEvent` remains focused on authoritative lifecycle
  evidence and does not change for this capability.
- The system gains a new versioned ownership contract, topic and Outbox event.
- API services that need order ownership add an idempotent consumer and a small
  local authorization projection.
- Ownership is replicated only to services with an explicit need, reducing
  unnecessary identity-data distribution.
- Authorization remains available when the Order Service is temporarily
  unavailable after the ownership projection has been built.
- Asynchronous propagation can temporarily deny a legitimate request until the
  projection catches up. This fail-closed behavior is accepted.
- Consumers must handle duplicate delivery, replay and contract evolution.
- Kafka ACLs and monitoring become part of the trust boundary for ownership
  propagation.
- A future identity/account capability must manage the mapping between
  external issuer-subject pairs and the internal customer identifier.
- A centralized policy engine may still be introduced for more complex
  policies, but simple resource ownership does not require one.

## Outcome

Order ownership is represented as an independent business fact rather than as
repeated lifecycle metadata or a transient security credential. Services that
expose order-derived APIs authorize requests using independently validated
tokens and local ownership projections, preserving data minimization, service
autonomy and a scalable path to additional customer-facing services.
