# Order Investigation Knowledge Corpus v1

## Purpose

This directory contains curated knowledge that can help explain an order
timeline and suggest appropriate investigation steps. It is supplemental
context for AI-supported explanations, not evidence about a particular order.
This README is the corpus manifest and must not itself be ingested as a
knowledge document.

The authoritative account of what happened to an order remains the lifecycle
evidence stored by the Investigation Service in PostgreSQL. Retrieved knowledge
must never add, remove, reorder, or override those facts.

## Corpus boundaries

The corpus includes:

- lifecycle and evidence semantics
- compensation procedures
- service ownership
- operational investigation guidance
- known failure patterns expressed as hypotheses

The corpus excludes:

- raw lifecycle events or application logs
- customer, payment, or inventory records
- transaction, command, correlation, or idempotency identifiers
- credentials, secrets, and provider payloads
- unreviewed incident descriptions

## Document metadata

Every knowledge document starts with metadata that the future ingestion process
must preserve:

- `id`: stable document identity
- `title`: human-readable source title used in citations
- `version`: version of this document
- `documentType`: knowledge category
- `services`: relevant services
- `statuses`: relevant order statuses
- `reasonCodes`: relevant lifecycle reasons
- `decisionCodes`: relevant orchestration decisions
- `effective`: whether the version is eligible for retrieval

Document identity and version are independent from the prompt version and from
the corpus directory version. A document can evolve without forcing an
unrelated prompt change.

## Interpretation rule

Lifecycle evidence answers **what happened to this order**. This corpus answers
**what the recorded facts mean and what an operator may inspect next**. When
the two appear to conflict, lifecycle evidence always wins.
