# ADR 0008: Separate Kubernetes Secrets Per Service

## Status
Accepted

## Context

The system is deployed as a set of independently deployable microservices:

- Order Service
- Inventory Service
- Payment Service
- Notification Service

Each service owns its own PostgreSQL database.

Initially, a shared Kubernetes Secret was considered for storing all database credentials.
While operationally simple, this approach violates the Principle of Least Privilege.

In such a design:
- every service deployment could potentially reference all database passwords
- accidental misconfiguration could expose credentials of unrelated services
- service boundaries become weaker

The system follows a domain-oriented architecture where each service owns its persistence layer and should not have access to the credentials of other services.

## Decision

A dedicated Kubernetes Secret will be created for each service/database pair.
The system will use:

- order-db-secret
- inventory-db-secret
- payment-db-secret
- notification-db-secret

Each secret contains only the password required by the corresponding service.
Services and database deployments will reference only their own secret.

## Rationale
This decision enforces service isolation at the infrastructure level.
Each service receives only the credentials required for its own operation.

Benefits include:
- reduced blast radius in case of misconfiguration
- stronger alignment with bounded contexts
- clearer ownership model
- easier migration to external secret management solutions later

The design also aligns naturally with future cloud deployments where secrets may be managed through cloud-based secret manager.

## Consequences
Benefits:
- improved security posture
- stronger service boundaries
- easier secret rotation
- simplified ownership

Trade-offs:
- additional Kubernetes Secret resources
- slightly more deployment maintenance

The increase in configuration is small and acceptable.

## Outcome
The system will:
- use one Kubernetes Secret per service/database pair
- avoid shared database credential storage
- maintain clear ownership boundaries across services