# ADR-0002 — Room 3 (androidx.room3) as the local SSOT

## Status
Accepted (Phase 3)

## Context
The blueprint requires Room 3 (`androidx.room3:*`, plan section 386).
Room 3 is a new maven group (`androidx.room3`) and package
(`androidx.room3.*`) with Kotlin-first codegen. The legacy
`androidx.room:room-compiler:2.8.5` generated code that did not compile
against Kotlin 2.4.20 in this toolchain.

## Decision
`core:database` uses `androidx.room3:room3-runtime:3.0.3` +
`room3-compiler` via KSP. Room 3 has no separate `-ktx` artifact:
transaction helpers are `withWriteTransaction`/`withReadTransaction`
in `androidx.room3` (replacing Room 2's `withTransaction`). Schema JSON
is exported to `schemas/` and committed (plan sections 21/228).
`fallbackToDestructiveMigration` is acceptable only while the schema
version is 1 and no users exist.

## Consequences
- Generated code is Kotlin-first and compiles cleanly with Kotlin 2.4.20.
- Any future migration to/from Room 2 is a package-level rename (documented).
- DAOs and entities stay in `androidx.room3.*` imports; feature code never
  touches Room types directly (repositories translate to core:model).
