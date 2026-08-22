# ADR 0013: Use Spring AI for LLM Integration

## Status
Accepted

## Context

The Investigation Service produces an order explanation from authoritative
timeline evidence. It requires an integration with a third-party LLM while
preserving deterministic validation and fallback behaviour when a generated
response is unavailable or invalid.

The integration should fit the existing Spring-based architecture, support
structured responses and observability, and avoid spreading provider-specific
contracts through application logic. The selected approach must also preserve
the ability to change the provider or model without redesigning the
Investigation Service.

The repository currently uses a Spring Boot version that is not compatible
with the preferred Spring AI release. Adopting Spring AI therefore includes a
platform upgrade whose impact must be evaluated across the complete system.

## Decision

Spring AI is the preferred framework for integrating the Investigation Service
with an LLM.

The Investigation Service retains an application-owned boundary around the AI
integration. This keeps the explanation workflow independent of both Spring AI
and the selected model provider.

Spring AI is selected because it provides:

- alignment with the existing Spring ecosystem
- abstractions across supported model providers
- support for structured responses, observability and evaluation
- reusable support for common AI integration concerns
- lower migration effort when changing models or providers

Changing a provider or model is not assumed to be configuration-only. Its
compatibility, quality, latency and cost must still be evaluated.

## Alternatives

### Provider Java SDK

A provider SDK offers direct access to one provider with less framework
involvement, but leaves more provider-specific integration responsibilities
within the system.

This is not the preferred initial approach because it provides less reusable
support for the AI integration concerns required by the Investigation Service.
It remains the fallback if adopting Spring AI requires an unacceptable
platform migration.

### Direct HTTP adapter

A direct HTTP integration minimizes framework dependencies and provides
complete control over communication with the provider.

It is not selected because the service would own more provider-specific
integration concerns. That work provides limited value compared with the
abstractions already available in Spring AI.

## Consequences

- The Investigation Service gains reusable AI integration and observability
  capabilities.
- Provider-specific details remain isolated from application and domain code.
- Common model or provider changes require less integration code, but still
  require compatibility and quality evaluation.
- The system adds Spring AI as a framework dependency in addition to the
  third-party model provider.
- The Spring Boot platform must be upgraded to a version compatible with the
  selected Spring AI release.
- The platform upgrade can affect all services and requires broader regression
  testing than the Investigation Service alone.
- Spring AI does not replace application-owned reliability, validation and
  cost controls.

## Future Considerations

The Spring Boot and Spring AI upgrade will be introduced incrementally. Each
step will be verified through the existing test suites and relevant service
integration checks before the LLM integration is enabled.

If the upgrade proves disproportionately complex, introduces unacceptable
system degradation or cannot preserve existing behaviour, the implementation
will return to the provider SDK alternative. The application-owned integration
boundary ensures that this fallback does not require redesigning the
explanation workflow or its public API.

## Outcome

The Investigation Service uses Spring AI as its preferred LLM integration
mechanism while retaining an application-owned boundary and a reversible path
to a provider SDK if the required platform upgrade is not acceptable.
