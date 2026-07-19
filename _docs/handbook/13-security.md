# Security

## Purpose

A microservice system exposes multiple potential attack surfaces, including service endpoints, databases, message brokers, monitoring components, and runtime credentials.

Security therefore cannot rely on a single mechanism. The deployment applies several complementary controls to limit external exposure, restrict communication inside the cluster, isolate credentials, and reduce container privileges.

The central principle is:

> Expose only what must be exposed, allow only required communication, and provide every workload with only the access it needs.

## Controlled Service Exposure

Application services are deployed using the Kubernetes `ClusterIP` service type.

A `ClusterIP` service is reachable only from within the Kubernetes cluster and is not directly exposed through a public load balancer.

This keeps internal components private by default, including:

* Payment Service
* Inventory Service
* Notification Service
* PostgreSQL databases
* Kafka
* internal monitoring components

External access should be introduced deliberately through a controlled entry point such as an Ingress, API Gateway, or external load balancer.

Using `ClusterIP` reduces accidental exposure, but it does not by itself authenticate or encrypt internal traffic. It represents the first boundary in a broader security model.

During the design phase, principles aligned with a zero-trust architecture were considered, such as enforcing encrypted communication (for example, TLS between services and Kafka) and requiring explicit authentication for every interaction. However, as this project is a side project with limited scope, these mechanisms were not fully implemented. Despite this, the design acknowledges the benefits of zero-trust approaches, including stronger protection against lateral movement, improved data confidentiality, and more robust identity-based access control, and these considerations inform how the system could be further hardened in a production environment.

## Internal Network Segmentation

Being inside the same Kubernetes cluster does not automatically mean that every workload should be allowed to communicate with every other workload.

NetworkPolicies define which connections are permitted between services, databases, Kafka, and supporting infrastructure.

For example:

```text
Order Service ─────────► Order Database
      │
      └───────────────► Kafka

Inventory Service ─────► Inventory Database
        │
        └─────────────► Kafka

Payment Service ───────► Payment Database
       │
       ├──────────────► Kafka
       └──────────────► External Payment Provider

Notification Service ──► Notification Database
          │
          └───────────► Kafka
```

Communication outside the defined paths is denied.

This prevents scenarios such as:

```text
Order Service ─────✕────► Payment Database
Payment Service ───✕────► Inventory Database
Notification Service ─✕─► Order Database
```

Network segmentation provides several benefits:

* reduced lateral movement if a workload is compromised
* smaller security blast radius
* infrastructure-level enforcement of database ownership
* prevention of accidental cross-service dependencies
* clearer visibility into allowed communication paths

Service boundaries are therefore enforced not only through application code, but also through the cluster network.

In this context, the use of NetworkPolicies represents a middle ground between a fully implemented zero-trust architecture and a simpler model that relies solely on implicit trust between internal services. While zero-trust would require strict identity verification and encryption for every interaction, and a basic internal model would allow unrestricted communication within the cluster, NetworkPolicies introduce explicit allow-listing of traffic paths. This approach strengthens security by reducing unnecessary connectivity and limiting lateral movement, without requiring the full complexity of identity-based enforcement and encrypted service-to-service communication.

## Secret Management

Sensitive configuration is injected through Kubernetes Secrets rather than stored directly in application configuration.

Each service has a dedicated secret for its own database credentials:

* `order-db-secret`
* `inventory-db-secret`
* `payment-db-secret`
* `notification-db-secret`

Services reference only the Secret associated with their own database.

For example:

```text
Order Service ───────► order-db-secret
Inventory Service ───► inventory-db-secret
Payment Service ─────► payment-db-secret
Notification Service ► notification-db-secret
```

A shared Secret containing all database credentials was intentionally avoided because it would weaken service isolation and increase the impact of accidental exposure.

Using separate Secrets supports the Principle of Least Privilege:

> A service receives only the credentials required to perform its own responsibility.

This reduces the blast radius of a compromised service and allows credentials to be rotated independently.

The Secret manifests stored in the repository contain placeholder values only. Real credentials must be supplied during deployment and must not be committed to source control.

For production environments, this design can be extended with a dedicated external secret-management solution and encryption of Kubernetes Secrets at rest.

## Configuration Separation

Sensitive and non-sensitive configuration are handled separately.

Kubernetes ConfigMaps contain ordinary runtime configuration such as:

* database host
* database port
* database name
* Kafka bootstrap address

Kubernetes Secrets contain sensitive values such as:

* database username
* database password

This separation improves clarity and prevents credentials from being treated like ordinary application configuration.

```text
ConfigMap
├── Database host
├── Database port
├── Database name
└── Kafka address

Secret
├── Database username
└── Database password
```

## Workload Hardening

Service containers run with reduced operating-system privileges.

The Kubernetes deployments apply controls such as:

* running the process as a non-root user
* disabling privilege escalation
* using a read-only root filesystem

These restrictions reduce the impact of a compromised application process.

Running as non-root prevents the application from receiving unnecessary administrative privileges inside the container. Disabling privilege escalation prevents the process from gaining additional permissions, while a read-only root filesystem limits the ability to modify the container image at runtime.

Together, these controls make containers more difficult to misuse and ensure that persistent data is written only through explicitly configured storage or external databases.

## Defence in Depth

The security model consists of several complementary boundaries:

```text
External Access Control
          ↓
ClusterIP Service Exposure
          ↓
NetworkPolicy Segmentation
          ↓
Per-Service Credentials
          ↓
Hardened Containers
```

No individual control is considered sufficient on its own.

For example:

* `ClusterIP` reduces public exposure but does not restrict all internal traffic.
* NetworkPolicies restrict internal communication but do not protect leaked credentials.
* Kubernetes Secrets protect configuration ownership but do not replace application authentication.
* Container security contexts reduce runtime privileges but do not validate incoming requests.

Security is achieved by combining these layers.

## Current Scope and Future Hardening

The current deployment provides protection through:

* private internal Kubernetes services
* restricted service-to-service communication
* isolated database credentials
* separation of configuration and secrets
* non-root containers
* disabled privilege escalation
* read-only container filesystems

As the system moves toward a production environment, additional controls would be required, including:

* authentication and authorization for external APIs
* TLS for external and internal communication
* Kafka authentication and encryption
* dedicated Kubernetes ServiceAccounts and RBAC
* container image vulnerability scanning
* image signing and verification
* Pod Security Admission
* external secret management
* secret rotation
* audit logging
* security monitoring and alerting

The current implementation establishes a secure deployment foundation, while these additional controls would extend protection across identity, transport, supply chain, and operational security.

## Diagrams

**System Exposure Model**
> ```mermaid
> graph TD
>     External[External Entry Point]
>     Ingress[Ingress / API Gateway]
>     Services[ClusterIP Services]
>     Kafka[Kafka]
>     RDS[AWS RDS Databases]
>
>     External --> Ingress
>     Ingress --> Services
>     Services --> Kafka
>     Services --> RDS
> ```


**Layered Security Model**
>
> ```text
> External Access Control
>           ↓
> ClusterIP Service Exposure
>           ↓
> NetworkPolicy Segmentation
>           ↓
> Per-Service Credentials
>           ↓
> Hardened Containers
> ```
