# Business Context

## Purpose

This project is a reference implementation of a production-oriented event-driven microservice architecture. The chosen domain—order processing—serves as a realistic business scenario to demonstrate architectural patterns commonly required in distributed systems.

Rather than focusing on business functionality, the project emphasizes architectural qualities such as reliability, scalability, observability, and maintainability. It illustrates how modern architectural practices can help organizations build systems that evolve safely as business requirements grow.

# Business Context Diagram

```mermaid
flowchart TB
    Customer["👤 Customer"]

subgraph System["Ordering System"]
OMS["🏢 Ordering System

Coordinates order placement,
payment processing,
inventory reservation,
and customer notification"]
end

PaymentProvider["💳 External Payment Provider"]

NotificationProvider["✉️ External Notification Provider"]

Customer -->|"Places an order"| OMS
OMS -->|"Order confirmation & status"| Customer

OMS -->|"Payment request"| PaymentProvider
PaymentProvider -->|"Payment result"| OMS

OMS -->|"Notification request"| NotificationProvider

style System fill:#eef4ff,stroke:#3b5fc0,stroke-width:2px

style OMS fill:#3b5fc0,stroke:#1f3a8a,color:#ffffff

style Customer fill:#f5f5f5,stroke:#888

style PaymentProvider fill:#fff4e5,stroke:#c98a1c
style NotificationProvider fill:#fff4e5,stroke:#c98a1c
```