---
name: review-change
description: Review code changes after implementation/modification to identify regressions, architectural violations, compatibility issues, security concerns, and missing test coverage before code is pushed to a remote branch.
---

# Goal

Perform a **code review only**. Do **not** modify code, generate patches, or automatically refactor implementation.

The purpose of this review is to identify risks, regressions, architectural violations, missing tests, security concerns, and backward compatibility issues before code is merged or pushed to a remote repository.

# Review Principles

## 1. Preserve Existing Architecture

Review changes against the existing architecture and design decisions.

Verify that:

- Existing architectural boundaries remain intact.
- Responsibilities are not moved into inappropriate layers.
- New code follows established project patterns.
- Temporary shortcuts are not introduced that increase future technical debt.
- New dependencies are justified and align with current architecture.

Flag any solution that introduces unnecessary architectural degradation.

---

## 2. Asynchronous Communication Rules

For any asynchronous communication, event-driven integration, messaging, or distributed workflow:

### Producer Side

Verify the producer follows the **Outbox Pattern**.

Check:

- Event publishing is transactionally consistent with state changes.
- Events cannot be lost between database commit and message publication.
- No direct broker publishing is introduced where Outbox is required.

### Consumer Side

Verify the consumer follows the **Inbox Pattern** where applicable.

Check:

- Duplicate message processing is prevented.
- Message tracking exists.
- Processing remains reliable during retries.

### Idempotency

Verify operations remain idempotent.

Check:

- Message redelivery does not cause duplicate side effects.
- Retry behavior is safe.
- Database writes remain protected against duplicate execution.
- Unique constraints or equivalent protections exist where required.

---

## 3. API Compatibility Review

All externally exposed APIs must remain backward compatible.

Review for:

### REST APIs

- Removed endpoints.
- Changed endpoint semantics.
- Modified request formats.
- Modified response formats.
- Renamed fields.
- Changed required/optional fields.
- Breaking validation changes.
- HTTP status changes impacting existing consumers.

### Events / Messages

- Breaking schema changes.
- Removed fields.
- Renamed fields.
- Contract incompatibilities.

### Database Contracts

- Changes that may break existing application behavior.
- Destructive schema modifications.
- Unsafe migrations.

Any breaking change must be reported.

---

## 4. Security Review

Verify that no security vulnerabilities are introduced.

Review for:

### Authentication

- Authentication bypasses.
- Missing authentication checks.
- Incorrect trust assumptions.

### Authorization

- Missing authorization validation.
- Privilege escalation possibilities.
- Resource ownership violations.

### Input Handling

- Injection vulnerabilities.
- Deserialization risks.
- Unsafe parsing.

### Data Protection

- Sensitive data leaks.
- Excessive logging of secrets.
- Exposure of internal implementation details.

### Transport & Integration

- Insecure communication.
- Missing validation of external data.
- Trusting client-provided identifiers without validation.

---

## 5. Spring Boot Alignment

For Spring Boot changes:

Verify that the implementation is aligned with the project's currently used Spring Boot version.

Check:

- APIs used are compatible with the current version.
- No features requiring newer versions are introduced accidentally.
- Deprecated constructs are not unnecessarily added.
- Security configuration follows practices supported by the current version.

Report version-related concerns.

---

## 6. Testing Requirements

### Bug Fixes

Every bug fix should include a regression test.

Verify:

- The reported issue can be reproduced by a test.
- The test fails before the fix.
- The test passes after the fix.
- Similar failure scenarios are considered.

Missing regression coverage is a review finding.

### New Functionality

Verify appropriate test coverage exists.

At minimum:

#### Happy Path

- Normal successful workflow.

#### Edge Cases

- Boundary conditions.
- Invalid inputs.
- Empty data.
- Duplicate requests.
- Concurrency/retry scenarios where relevant.
- Failure handling.

Avoid requiring excessive tests, but ensure meaningful coverage.

---

## 7. General Code Quality

Review for:

- Race conditions.
- Transaction consistency issues.
- Resource leaks.
- Error handling problems.
- Logging quality.
- Performance regressions.
- Excessive complexity.
- Dead code.
- Maintainability concerns.

---

# Required Output Format

Produce only the following review report.

## Review Report

### Blocking (Must Fix - Block Merge)

List findings that:

- Introduce regressions.
- Break existing contracts.
- Violate architecture significantly.
- Introduce security vulnerabilities.
- Risk data loss or corruption.
- Violate Outbox/Inbox requirements.
- Break idempotency.

Format:

- **[BLOCKING]** Description
    - Impact:
    - Recommendation:

---

### Important (Should Fix - Discuss if Disagree)

List findings that:

- Reduce maintainability.
- Increase operational risk.
- Have insufficient tests.
- Create future architectural concerns.
- Introduce version compatibility concerns.

Format:

- **[IMPORTANT]** Description
    - Impact:
    - Recommendation:

---

### Nit (Nice to Have)

Optional improvements.

Format:

- **[NIT]** Description
    - Reason:

---

### Positive Observations

List improvements worth keeping.

Format:

- Description

---

### Review Summary

Provide:

- Number of Blocking findings
- Number of Important findings
- Number of Nit findings
- Overall recommendation:
    - APPROVE
    - APPROVE WITH COMMENTS
    - REQUEST CHANGES

