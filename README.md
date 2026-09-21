# Spreva

[![CI](https://github.com/Alaa91H/SPREVA/actions/workflows/ci.yml/badge.svg)](https://github.com/Alaa91H/SPREVA/actions/workflows/ci.yml)
[![Release](https://github.com/Alaa91H/SPREVA/actions/workflows/release.yml/badge.svg)](https://github.com/Alaa91H/SPREVA/releases)
[![Latest release](https://img.shields.io/github/v/release/Alaa91H/SPREVA?include_prereleases&label=latest)](https://github.com/Alaa91H/SPREVA/releases/latest)
[![License](https://img.shields.io/badge/license-TBD-lightgrey)](#license)

> **Spreva** is a provisional project name pending trademark clearance.
> Tagline: *Speak. Learn. Live.* — German, that you actually speak.

Spreva is an offline-first Android app for learning German from Pre-A1 to C1,
with first-class Arabic support. This repository contains a production-grade,
32-module foundation plus the Phase 4 audio/learning layers.

**Status:**

- Phase 3 — Repository Bootstrap: **complete** (see [phase3-final-report.md](phase3-final-report.md))
- Phase 4.1 — Media3 audio foundation + built-in TTS: **complete**
- Phase 4.2 — Bundled native German recordings + listening activities: **complete**
- Phase 4.3 — Learner recording + shadowing (manual compare + waveform similarity): **complete**
- Phase 4 stabilization — onboarding/goal persistence, safe lesson completion,
  instruction-language policy, permission flow, recorder safety, media-registry
  integrity validation in CI: **complete**
- Academic German curriculum A1 → C1: **complete** — 83 units, 332 lessons,
  3,817 activities, German/Arabic/English instructional content, and curriculum integrity validation
  (see [docs/CURRICULUM_A1_C1.md](docs/CURRICULUM_A1_C1.md))
- Learning Intelligence: **complete foundation** — FSRS-6 scheduling, confidence-weighted skill/topic mastery,
  repeated-mistake remediation, adaptive A1–C1 placement, personalized focus recommendations, and local-first evidence analytics
  (see [docs/LEARNING_INTELLIGENCE.md](docs/LEARNING_INTELLIGENCE.md))
- Next: rubric-calibrated writing/speaking evaluation, personalized FSRS parameter optimization after sufficient review history,
  larger licensed native-audio coverage, and instrumented end-to-end tests

## Stack (all stable, verified 2026-09-18)

- Kotlin 2.4.20 · AGP 9.4.0 (built-in Kotlin) · Gradle 9.6.0 · JDK 17
- compileSdk/targetSdk 37 · minSdk 26
- Compose BOM 2026.09.00 (material3 1.4.0) · Navigation 3 1.1.7
- **Room 3** (`androidx.room3:3.0.3`) · DataStore 1.2.1 · Hilt 2.60.1
- Details + deviations: [docs/implementation/dependency-snapshot.md](docs/implementation/dependency-snapshot.md)

## Build

```bash
# Requirements: JDK 17 (JAVA_HOME set), Android SDK (local.properties or ANDROID_HOME)
./gradlew assembleDebug     # debug APK
./gradlew test              # unit tests
./gradlew assembleRelease   # R8-minified release
```

Open in Android Studio, pick a device/emulator, run `:app`. The learning flow:
Welcome → language/goal → Home → Learn → choose A1/A2/B1/B2/C1 → open a unit
and lesson → complete explanation, vocabulary, retrieval and production
activities → Practice → personalized mastery/focus dashboard → FSRS reviews
(vocabulary + repeated-error remediation) or adaptive placement → restart:
progress and the latest starting-level recommendation persist.
Theme (light/dark/system/dynamic) and UI language (English/العربية) switch
live from Settings.

## Architecture

- **feature api/impl split** — feature impls never see each other (ADR-0001)
- **Room 3 SSOT + event log** — offline-first writes, append-only attempt/review history,
  mastery and mistake intelligence derived from evidence rather than duplicated state (ADR-0002)
- **Navigation 3** — type-safe keys, app-owned entry provider (ADR-0003)
- **Convention plugins** — one-line module setup (ADR-0004)
- **FSRS-6 memory engine** — 21-parameter default model, 90% target retention,
  versioned opaque scheduler state, lazy migration from legacy demo cards,
  and interval previews before grading (ADR-0005 compatibility retained)
- **Content-as-data** — bundled JSON course; complete A1–C1 academic path (332 lessons / 3,817 activities), parser + curriculum/media integrity gates; downloadable packages later

Module map, dependency graph, DB schema and next steps:
[phase3-final-report.md](phase3-final-report.md).

## CI & Releases

- **CI** (`.github/workflows/ci.yml`): `test` + `assembleDebug` on every PR
  and push to `main`, with test-report artifacts. Build/dependency caches
  are stored on `main` and reused by PRs.
- **Release** (`.github/workflows/release.yml`): on every `v*` tag — runs
  tests, builds debug + R8-minified release APKs and attaches them
  (`Spreva-<version>-debug/release.apk`) to the GitHub Release.

## Download

Grab the latest APK from
[Releases](https://github.com/Alaa91H/SPREVA/releases/latest)
(`Spreva-<version>-debug.apk` is the daily-build artifact; the release APK
is R8-minified and debug-signed until the signing workflow lands).

## License

To be decided before any public distribution. All rights reserved by the
author until then.
