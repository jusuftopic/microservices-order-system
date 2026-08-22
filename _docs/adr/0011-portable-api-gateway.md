# ADR 0011: Use a Portable API Gateway

## Status
Accepted

## Context

The Order and Investigation APIs need a single external entry point with
path-based routing. The boundary must also protect internal services and limit
traffic to the Investigation API because third-party LLM calls can be slow and
costly when requests are excessive.

The same gateway approach must support local development and Kubernetes
deployment without requiring different application service or client designs.

## Decision

Traefik OSS is the API gateway.

During local development, Traefik uses file-based configuration and routes
requests by path to private service containers. In Kubernetes, Traefik acts as
a controller with the Kubernetes Gateway API as the routing contract. Both
environments preserve the same public paths and gateway responsibilities while
using their platform-appropriate configuration model.

The gateway is responsible for:

- providing one public entry point
- routing requests to the correct API by path
- applying rate limits, with stricter protection for LLM-backed operations
- keeping application services from being exposed directly

Business rules, authorization decisions, request validation and validation of
LLM-generated responses remain application responsibilities.

## Alternatives

### Expose each service directly

Rejected because it distributes edge controls across services and exposes more
of the internal architecture to clients.

### Use Spring Cloud Gateway

Rejected because a separate application runtime is unnecessary for the routing
and rate-limiting needs and overlaps with the Kubernetes edge component.

### Use a Kubernetes-only gateway

Rejected because it gives local development a different entry path and leaves
the Investigation API without the same protection in every environment.

## Consequences

- Clients use stable paths through one entry point in every environment.
- Internal APIs have a smaller external attack surface.
- Central rate limiting helps contain abuse and third-party LLM cost.
- Traefik OSS limits apply per gateway instance, so hard LLM usage budgets must
  also be enforced by the application or provider.
- The gateway becomes an operational dependency and must be observable.
- File-based local configuration is not identical to Kubernetes resources, but
  the public routes and responsibilities remain the same.
- Changing the gateway implementation can require adaptation of
  Traefik-specific policies.

## Outcome

The system uses a lightweight gateway consistently across local development
and Kubernetes without coupling business services to the gateway.
