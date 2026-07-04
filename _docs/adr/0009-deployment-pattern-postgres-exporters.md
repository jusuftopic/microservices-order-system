# ADR 0009: Deployment Pattern for Prometheus PostgreSQL Exporter

## Status
Accepted

## Context
The system utilizes a PostgreSQL database layer to manage stateful data. To monitor database health, performance, and resource usage, we are integrating Prometheus monitoring using the `postgres_exporter`.

It needs to decided how to deploy this exporter within our infrastructure. Two primary deployment topologies are available:
1. **Sidecar Pattern:** Running the exporter as a secondary container inside the database Pod.
2. **Separate Pod Pattern:** Running the exporter as an independent deployment within the Kubernetes cluster, connecting to the database over the internal network.

Target environment is an AWS-managed ecosystem, and we aim to follow production-grade architecture that maximizes operational reliability, cloud efficiency, and clean infrastructure boundaries.

## Decision
We will deploy the `postgres_exporter` using the **Separate Pod Pattern** in conjunction with **AWS RDS (PostgreSQL)** for the database layer.

The database will live outside the Kubernetes cluster as a fully managed AWS RDS instance. The Prometheus exporter will run as a lightweight, independent single-container Deployment inside the cluster's `monitoring` namespace, establishing a secure network connection to the RDS endpoint over private AWS VPC routing.

## Rationale
This decision aligns monitoring strategy with a cloud architecture and production realities:

* **Managed Database Superiority:** Running PostgreSQL inside Kubernetes requires complex stateful management (Operators, PVCs, StorageClasses). Offloading the database to AWS RDS eliminates operational overhead (automated backups, multi-AZ high availability, automatic OS patching) and qualifies for the AWS Free Tier (e.g., `db.t3.micro`), keeping side-project cloud costs minimal.
* **Inherent Architecture Compatibility:** Because AWS RDS is a managed service external to the cluster, we cannot inject a sidecar container into the database host. The Separate Pod pattern is the only logical architecture that supports a managed database service.
* **Resource and Fault Isolation:** A runaway or heavy analytical monitoring query executed by the exporter will consume CPU and memory isolated entirely to the exporter's pod limits. It cannot drain resources from the primary database or trigger an accidental database crash.
* **Independent Lifecycles:** We can update, patch, restart, or reconfigure the exporter deployment at any time without triggering a rolling restart or causing brief downtime for the database layer.

## Consequences
Benefits:
* **Production Realism:** Demonstrates architectural maturity by offloading state to managed cloud infrastructure while maintaining a stateless Kubernetes compute layer.
* **Total Resource Isolation:** Zero risk of monitoring utilities impacting database performance or availability.
* **Simplified Upgrades:** Decoupled lifecycles mean infrastructure maintenance on the monitoring stack does not threaten database uptime.
* **Cost Efficiency:** Leverages native cloud database tiers without overloading EKS worker node storage with expensive EBS volumes.

Trade-offs:
* **Network & Configuration Overhead:** Requires managing AWS Security Groups to allow inbound `5432` traffic from EKS worker nodes to the RDS instance.
* **Security & Credential Management:** Introduces the need to securely route database credentials across network boundaries via a dedicated Kubernetes Secret, rather than binding strictly to a local `127.0.0.1` address.

The trade-off of network configuration overhead is entirely acceptable given the massive gains in database reliability and operational simplicity provided by a managed service.

## Outcome
The system will:
* Provision a managed AWS RDS PostgreSQL instance in the cluster's VPC.
* Deploy the `postgres_exporter` as an independent, single-container Deployment in Kubernetes.
* Enforce secure network boundaries using AWS Security Groups and encrypted database connections (`sslmode=require`).
* Inject database credentials into the exporter pod using a native Kubernetes Secret (`postgres-db-secret`).