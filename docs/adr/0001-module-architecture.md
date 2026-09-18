# ADR-0001 — Module architecture: vertical features + api/impl split

## Status
Accepted (Phase 3)

## Context
The product roadmap (Phase 2 blueprint) spans many learning features
(vocabulary, grammar, speaking, …). A single-module app or a flat feature
list would couple features to each other and slow incremental builds.

## Decision
Every user-facing capability lives in `feature/<name>/api` (nav keys and
composable entry contracts only) and `feature/<name>/impl`
(screen + ViewModel + DI binding). Feature impl modules depend on other
features' **api** modules only — never on another impl. Domain and core
layers never depend on features. The `app` module assembles features
through Hilt-bound `Entry` contracts (plan sections 7/8/37).

## Consequences
- Parallel, independent feature development; enforced by Gradle boundaries.
- Slightly more modules and boilerplate per feature (accepted).
- Architecture violations are caught at compile time, not review time.
