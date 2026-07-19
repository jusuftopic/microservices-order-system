# Testing Strategy

## Purpose

Testing a distributed system requires more than verifying individual methods or classes.

Each microservice contains its own business logic, persistence layer, messaging integration, and architectural boundaries. At the same time, the overall business workflow depends on several services collaborating correctly through asynchronous communication.

The testing strategy therefore combines multiple complementary test layers. Each layer protects the system against a different category of failure.

The current implementation focuses on:

* unit tests
* integration tests
* architecture tests

Additional testing capabilities such as end-to-end, regression, contract, and load testing are considered future extensions as the system and its operational requirements grow.

## Unit Tests

Unit tests verify individual components in isolation.

They focus primarily on business decisions and application behaviour without requiring databases, Kafka, or external providers.

Examples include:

* order state transitions
* inventory reservation decisions
* payment result handling
* compensation decisions
* event creation
* notification processing logic

Dependencies are mocked where appropriate, allowing failures to be traced directly to the tested component.

Because unit tests are fast and focused, they provide immediate feedback during development and form the foundation of the testing strategy.

## Integration Tests

Integration tests verify that application behaviour continues to work when real framework configuration and infrastructure components are involved.

Unlike unit tests, these tests validate collaboration between several parts of a service.

Examples include:

* repository persistence against a real database
* transaction boundaries
* transactional creation of business data and Outbox records
* Inbox processing and duplicate-message protection
* Kafka message publication and consumption
* service configuration
* Spring context integration
* idempotent payment processing

Integration tests are particularly important in this architecture because many reliability guarantees depend on infrastructure behaviour.

For example, the Outbox Pattern is not protected only by verifying that an event object can be created. The test must also confirm that the business entity and its corresponding Outbox record are persisted within the same transaction.

Similarly, Inbox processing must verify that an incoming message cannot be processed more than once, even when Kafka delivers it repeatedly.

These tests provide confidence that the reliability mechanisms operate correctly beyond isolated application logic.

## Architecture Tests

Architecture tests protect the intended structure of the system.

They do not verify a business result. Instead, they verify that the code continues to respect the architectural decisions defined for the project.

ArchUnit is used to express these decisions as executable rules.

This transforms architectural documentation into continuously verified constraints and prevents gradual architectural degradation as the codebase evolves.

### Layer Boundaries

Architecture tests enforce basic dependency rules between layers, ensuring that controllers depend on application services, which in turn depend on repositories and infrastructure components. This prevents unintended coupling, such as controllers accessing persistence directly or domain logic depending on web-specific code.

### Service Independence

Architecture tests ensure that microservices remain isolated by preventing direct imports of internal classes from other services. Communication is expected to happen through Kafka events or defined API contracts, preserving independent evolution and avoiding tight coupling.

### Shared Infrastructure Boundaries

Architecture tests verify that shared modules like `messaging-starter` remain focused on technical concerns and do not accumulate service-specific business logic, helping to avoid hidden coupling across services.

### Package and Naming Conventions

Architecture tests also enforce basic structural conventions, such as keeping controllers, repositories, and messaging components in their respective packages, improving consistency and maintainability.

## Representative Protection Across the System

The existing test layers protect different risks across the microservices.

### Order Service

Tests protect:

* valid order creation
* order state transitions
* compensation outcomes
* persistence of order state
* publication of corresponding domain events

### Inventory Service

Tests protect:

* stock reservation
* reservation confirmation
* reservation cancellation
* duplicate event handling
* consistency between inventory changes and outgoing events

### Payment Service

Tests protect:

* payment state transitions
* provider-result handling
* idempotent payment execution
* retry and failure outcomes
* separation between local transactions and external provider calls

### Notification Service

Tests protect:

* correct event consumption
* notification request creation
* repeated message handling
* failure and retry behaviour

### Messaging Infrastructure

Tests protect:

* atomic Outbox persistence
* Inbox duplicate protection
* event publication
* Kafka consumption
* retry and dead-letter handling

### Architectural Structure

ArchUnit tests protect:

* layer dependency direction
* service independence
* domain isolation
* shared-module boundaries
* package and naming conventions

The goal is not to test every implementation detail. The goal is to protect the business rules, infrastructure guarantees, and architectural boundaries that are most important to system correctness.

## Testing as a CI Quality Gate

The testing strategy is integrated into the Continuous Integration pipeline.

For every affected service, the pipeline performs validation before producing a Docker image.

```text
Compile
   ↓
Unit Tests
   ↓
Integration Tests
   ↓
Architecture Tests
   ↓
Package
   ↓
Docker Image
```

A failing unit test, integration test, or architectural rule prevents the affected service from being treated as a valid deployment artifact.

This ensures that the pipeline protects not only functional correctness, but also the long-term architectural integrity of the system.

## Future Testing Capabilities

The current strategy provides strong protection at the service and architecture level.

As the system grows, additional test layers would provide confidence across complete workflows, compatibility boundaries, and production-like workloads.

## End-to-End Tests

End-to-end tests would verify complete business workflows across all participating services.

A successful order scenario could validate:

```text
Create Order
    ↓
Reserve Inventory
    ↓
Process Payment
    ↓
Complete Order
    ↓
Send Notification
```

A compensation scenario could verify:

```text
Create Order
    ↓
Reserve Inventory
    ↓
Payment Fails
    ↓
Release Inventory
    ↓
Mark Order as Failed
```

These tests would provide confidence that independently tested services still collaborate correctly as one distributed system.

Because end-to-end tests are slower and harder to diagnose, they should focus on a limited number of critical business journeys rather than replacing lower-level tests.

## Regression Tests

Regression tests would preserve behaviour that has previously failed or required correction.

Each significant defect or production incident should result in a permanent automated test whenever possible.

Examples include:

* duplicate Kafka delivery
* repeated payment-provider responses
* invalid state transitions
* compensation failures
* delayed messages
* provider timeouts
* partial workflow completion

Over time, this suite would become an executable record of previously discovered failure scenarios.

## Contract Tests

Contract tests would verify compatibility between independently evolving services and external providers.

They could protect:

* Kafka event schemas
* required and optional event fields
* backward compatibility
* HTTP request and response formats
* payment-provider expectations

This would allow producers and consumers to evolve independently while reducing the risk that a deployment breaks another service.

## Load and Performance Tests

Load tests would validate whether the system continues to meet its operational expectations under realistic and peak traffic.

Relevant measurements would include:

* order throughput
* response-time percentiles
* Kafka consumer lag
* database connection usage
* Outbox publication backlog
* retry volume
* Circuit Breaker behaviour
* payment-provider latency
* CPU and memory utilisation

A particularly important scenario would simulate payment-provider degradation.

The test should verify that:

* retries remain controlled
* the Circuit Breaker opens as expected
* the system avoids a retry storm
* unrelated services remain operational
* order processing degrades in a controlled manner

The purpose of load testing is therefore not only to determine the highest possible throughput. It is to understand how the architecture behaves when its limits are approached.

## Testing Strategy at Scale

At scale, the testing strategy can be viewed through three complementary forms of confidence.

### Functional Confidence

Protects expected business behaviour through:

* unit tests
* integration tests
* end-to-end tests
* regression tests

### Structural Confidence

Protects architectural integrity and compatibility through:

* ArchUnit tests
* contract tests

### Operational Confidence

Protects production behaviour under stress and failure through:

* load tests
* performance tests
* resilience scenarios
* failure injection

The current implementation establishes strong functional and structural protection at the individual service level.

Future testing capabilities would extend that confidence across complete business workflows, independent deployments, and production-scale operating conditions.
