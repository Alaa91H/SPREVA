# Spreva

> **Spreva** is a provisional project name pending trademark clearance.
> Tagline: *Speak. Learn. Live.* — German, that you actually speak.

Spreva is an offline-first Android app for learning German from Pre-A1 to C1,
with first-class Arabic support. This repository currently contains **Phase 3:
Repository Bootstrap** — a production-grade, 32-module foundation.

**Status:** `PHASE 3 STATUS: COMPLETE` — see [phase3-final-report.md](phase3-final-report.md)
and the honest NOT-RUN gates listed there.

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
./gradlew test              # unit tests (16)
./gradlew assembleRelease   # R8-minified release
```

Open in Android Studio, pick a device/emulator, run `:app`. The demo flow:
Welcome → language/goal → Home → Learn → A1 → "Hallo!" lesson (all five
activity types) → lesson completes → Practice → review cards for the
lesson's vocabulary (Again/Hard/Good/Easy) → restart: progress persists.
Theme (light/dark/system/dynamic) and UI language (English/العربية) switch
live from Settings.

## Architecture

- **feature api/impl split** — feature impls never see each other (ADR-0001)
- **Room 3 SSOT + event log** — offline-first writes, append-only history (ADR-0002)
- **Navigation 3** — type-safe keys, app-owned entry provider (ADR-0003)
- **Convention plugins** — one-line module setup (ADR-0004)
- **Demo scheduler → FSRS-ready** — versioned scheduler state (ADR-0005)
- **Content-as-data** — bundled JSON course; parser+validator tested; downloadable packages later

Module map, dependency graph, DB schema and next steps:
[phase3-final-report.md](phase3-final-report.md).

## CI

GitHub Actions (`.github/workflows/ci.yml`): `test` + `assembleDebug` on
every PR and push to `main`, with test-report artifacts.
