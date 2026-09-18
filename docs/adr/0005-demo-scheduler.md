# ADR-0005 — Demo memory scheduler before FSRS

## Status
Accepted (Phase 3)

## Context
The blueprint requires a `ReviewScheduler` interface (plan sections
77-79) with FSRS as the target algorithm, but Phase 3 ships offline demo
flows only. Pulling a full FSRS implementation into the foundation phase
would add risk without user value yet.

## Decision
`core:memory` defines `ReviewScheduler` (`preview`, `grade`, `version`)
and ships `DemoReviewScheduler` (`demo-v1`): fixed ladder
Again → 10 min; Hard/Good/Easy → 1/3/7/14-day ladder, resetting on
Again. Cards persist `schedulerVersion` and opaque JSON
`schedulerState`, so a future FSRS migration can recompute or interpret
state without schema changes (plan section 79).

## Consequences
- UI/DB never see algorithm details; swapping in FSRS touches only
  `core:memory` + DI provider.
- Demo intervals are honest placeholders, documented in-app as such.
- Every review log row stores the scheduler version that produced it.
