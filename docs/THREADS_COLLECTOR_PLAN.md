# Threads Collector Plan

## Scope

This plan defines the future Threads collector contract and extraction pipeline. It does not implement browser execution, Threads access, GraphQL calls, authentication, scheduling, or persistence.

## Collector Contract

- `ThreadsCollector` accepts a `ThreadsCollectionRequest`.
- The request is limited to active `SourceType.THREADS` profile sources using `https://threads.com/@...` or `https://www.threads.com/@...` URLs.
- The result returns extracted post candidates as raw URL/content pairs plus non-fatal warnings.
- Conversion from extracted post candidates to `CollectedItem` remains a later persistence step.

## Planned Extraction Pipeline

1. Receive a Threads profile source URL.
2. Render the public profile page in an isolated browser context.
3. Store the rendered DOM.
4. Remove `script` and `style` blocks.
5. Strip HTML tags into text lines.
6. Decode HTML entities.
7. Remove empty, duplicate, and layout-only lines.
8. Estimate post boundaries from visible text structure.
9. Return post candidates for later hashing and persistence.

## Design Constraints

- Do not depend on stable GraphQL `doc_id`, relay operation names, bundle names, or DOM class names.
- Do not request or store user passwords.
- Do not export browser cookies or session data.
- Treat login-required continuation text as a warning, not as a hard failure.
- Keep collection limits explicit to avoid accidental large-scale collection.

## Deferred Implementation

- Headless Chrome execution
- Local Chrome profile/session handling
- DOM parsing implementation
- Post boundary heuristics
- `CollectedItem` persistence
- Collection run orchestration
- UI trigger for actual collection
