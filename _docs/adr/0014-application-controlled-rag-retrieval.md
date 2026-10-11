# ADR 0014: Use Application-Controlled Retrieval for Initial RAG

## Status
Accepted

## Context

The Investigation Service answers questions using authoritative order evidence
and reusable investigation knowledge. A question by itself may be too generic
to retrieve the knowledge relevant to a particular order.

The system must decide whether retrieval is controlled by the application or
delegated to the language model. The initial design should remain predictable,
observable and straightforward to evaluate while the retrieval capability is
still being learned and validated.

## Decision

The application performs a knowledge search for every valid investigation
question before invoking the language model.

The search query combines the user's question with a brief, normalized subset
of authoritative timeline evidence. This may include the current status,
reason, orchestration decision and compensation type. Raw timelines and
order-specific evidence are not stored in the vector store.

Only knowledge that satisfies the configured relevance criteria is supplied to
the language model. An empty search result is valid and no unrelated knowledge
is added merely to provide context.

This is an initial retrieval policy rather than a permanent requirement that
every future question must perform a search. Its effectiveness will be assessed
through retrieval evaluation and operational observations.

## Alternatives

### Language-model tool calling

Knowledge search could be registered as a tool so that the language model
decides whether a search is needed and can formulate the search request.

This is not selected initially because it makes retrieval decisions
non-deterministic and increases evaluation, latency, cost and failure-handling
complexity. It remains a future option for questions that benefit from
conditional or iterative retrieval.

### Application intent routing

The application could classify questions and skip retrieval when authoritative
timeline evidence is sufficient or when a question is outside the supported
scope.

This is deferred until evaluation data demonstrates useful and stable question
categories. Introducing a router now would add another decision model before
the basic retrieval path has been evaluated.

### Retrieval without evidence enrichment

The vector search could use only the user's question. This is simpler, but
generic questions such as why an order failed may not contain enough context to
select the correct failure or compensation knowledge.

## Consequences

- Retrieval behavior is predictable and controlled by the application.
- Brief evidence improves searches for generic order questions.
- Authoritative evidence remains separate from reusable vector knowledge.
- Every valid question currently incurs an embedding and search operation.
- Evidence enrichment may influence retrieval even when a question concerns a
  different aspect of the order, so retrieval quality must be evaluated.
- Tool-based or intent-routed retrieval can be introduced later without
  changing ownership of authoritative order evidence.

## Future Considerations

Evaluation should compare application-controlled retrieval with question-only
search, intent routing and language-model tool calling. The policy should be
revisited if unconditional retrieval reduces answer quality or creates
unacceptable latency or cost.

## Outcome

The first RAG version uses application-controlled retrieval enriched with
brief authoritative evidence, while preserving tool-based search as an
explicit future architectural option.
