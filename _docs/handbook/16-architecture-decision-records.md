# Architecture Decision Records

## Purpose

The implementation shows **how** the system works, but it does not always explain **why** a particular architectural approach was selected.

Significant architectural decisions are therefore documented as **Architecture Decision Records (ADRs)**. They preserve the context, reasoning, alternatives, and consequences behind decisions that influence the system beyond a single implementation change.

The ADRs are stored in:

```text
_docs/adr
```

This keeps architectural decisions close to the source code and allows them to evolve together with the system.

---

## ADR Scope

Not every technical decision requires an ADR.

An ADR is created when a decision:

* has a long-term architectural impact
* affects multiple services or infrastructure components
* introduces meaningful benefits and trade-offs
* establishes a pattern that future implementation should follow
* would be difficult or expensive to reverse
* requires context that cannot be understood from the code alone

Small implementation details, refactorings, and temporary development choices are not documented as ADRs.

---

## ADR Structure

Each ADR follows a lightweight and consistent structure:

* **Status** — whether the decision is proposed, accepted, deprecated, or superseded
* **Context** — the problem, constraints, and forces influencing the decision
* **Decision** — the selected architectural approach
* **Rationale** — why the approach was chosen
* **Alternatives** — other options that were considered
* **Consequences** — the benefits, trade-offs, and operational implications
* **Outcome** — how the decision shapes the resulting architecture

This structure makes each decision understandable without requiring the reader to reconstruct its history from commits or implementation details.

---

## Decision Philosophy

The project follows an **evolutionary architecture** approach rather than pursuing architectural purity independently of practical constraints.

Architectural decisions balance:

* reliability
* service autonomy
* operational simplicity
* infrastructure cost
* security
* observability
* future scalability
* scope and project environment 

Some decisions intentionally reflect the current project phase. For example, sharing one PostgreSQL instance is accepted for cost efficiency, while separate databases and credentials preserve a future path toward stronger physical isolation.

The objective is not to select the theoretically most advanced solution in every situation. The objective is to make deliberate decisions whose constraints and trade-offs are understood and documented.

---

## Evolution of Decisions

ADRs are treated as historical records and should not be silently rewritten when the architecture changes significantly.

When an existing decision is replaced:

1. the original ADR remains in the repository
2. its status is changed to `Superseded`
3. a new ADR documents the new context and decision
4. both ADRs reference each other

This preserves the architectural history of the system and explains not only what the current design is, but also how and why it evolved.
