# ADR-0004 — Precompiled script plugins for application/feature conventions

## Status
Accepted (Phase 3)

## Context
AGP 9 ships built-in Kotlin support (no separate
`org.jetbrains.kotlin.android` plugin). During bring-up, class-based
convention plugins applied through the included build did not propagate
`implementation`/`ksp` dependency additions to modules reliably, producing
confusing `MissingBinding`/unresolved-symbol errors. Precompiled script
plugins, by contrast, receive the full plugin classpath — but their
`plugins {}` blocks require the corresponding **plugin markers** to be
real (implementation) dependencies of `build-logic`.

## Decision
- `spreva.android.application` and `spreva.android.feature` are
  **precompiled script plugins** (`build-logic/convention/src/main/kotlin/
  spreva.android.*.grad.kts`) carrying the full shared wiring
  (Hilt, Compose BOM, lifecycle, core modules for features).
- Remaining conventions (library, compose, room, hilt, testing, jvm)
  stay class-based and composed.
- `build-logic/convention/build.gradle.kts` declares `implementation`
  dependencies on the plugin markers (android app/library, compose
  compiler, serialization, hilt, ksp jar) so precompiled scripts resolve.

## Consequences
- App/feature modules get identical, complete setup from one line.
- Plugin id/implementation registration for these two is automatic
  (from file names) — the manual registry deliberately omits them.
- Any future class-based convention must re-verify dependency propagation
  before adoption.
