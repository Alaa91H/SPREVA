# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **In-app media attributions (Settings → About)** — Settings/About section opens a bottom sheet listing every bundled recording with its CC BY-SA 4.0 attribution and a link to its Wikimedia Commons source page, served dynamically from `licenses.json` through the content pipeline (parser → repository → use case). Fulfills the CC BY-SA attribution requirement inside the app itself (plan sections 99-100, 253).
- **Listening comprehension activity (`listening_choice`)** — the learner plays a bundled native recording and picks the matching phrase from options; the correct answer is never spoken by TTS in this activity. Added “Wie heißt du?” recording (CC BY-SA 4.0, Jeuwre) plus a fifth lesson activity and a new informal-register vocab item; parser validates the new activity type (2 new tests).
- **Phase 4.2 — bundled course audio (Media3 ExoPlayer) with native-speaker recordings**
  - Content schema: optional `audio` field on vocabulary items (path relative to `content/`); lesson 1 ships four native German recordings (Hallo / Guten Morgen / Ich heiße / Tschüss).
  - `core:audio`: `CourseAudioPlayer` contract + `ExoPlayerCourseAudioPlayer` (Media3 1.11.1) and a pure `CourseAudioLocator` resolving content paths to `asset:///` URIs (JVM-tested, 4 cases).
  - Playback priority: bundled native recording first, built-in TTS fallback when a word has no recording (plan section 65) — live-demonstrated by the “Wie heißen Sie?” item.
  - Provenance registry `content/audio/licenses.json` per plan sections 99-100: Wikimedia Commons, **CC BY-SA 4.0**, recordings by Jeuwre (native speaker, Berlin); GFDL/NC-licensed candidates deliberately excluded.
- **Phase 4.1 — audio foundation (Media3) + built-in TTS for German audio in lessons**
  - New `core:audio` module: `TtsProvider` contract (`AndroidTtsProvider` using the platform `TextToSpeech` engine, `de-DE`), `SpeechText` normalization (strips translation hints/punctuation, composes article + noun utterances), and `TtsStatus` availability flow (`NOT_READY / READY / MISSING_GERMAN`).
  - Lesson screen: header speaker button repeats the German prompt of the current activity; vocabulary cards gained per-word speaker buttons (article + noun). Buttons render only when a German voice is available.
  - Media3 `1.11.1` (common/exoplayer/session) added to the catalog and `core:audio` — playback wiring lands in Phase 4.2.
  - New icons: `material-icons-extended` added to the lesson module for `VolumeUp` (R8 strips unused vectors).
  - Unit tests: `SpeechTextTest` (7 cases).
- ADR-0006 documents the provider split and the `Speaker` → `TtsProvider` supersession.

### Changed
- CI: unit tests and `assembleDebug` merged into a single Gradle invocation (one configuration pass, shared compiled classes); superseded runs on the same branch/PR are now auto-cancelled; the release workflow uses the same single-invocation pattern.

## [0.1.0-alpha03] — 2026-09-18

Initial public bootstrap of the Spreva Android application: the complete
Phase 3 foundation — a 32-module, offline-first German-learning app shell
with a fully working demo lesson, review loop and persistence.

### Added

#### Toolchain & build
- **AGP 9.4.0 with built-in Kotlin support** (no separate
  `org.jetbrains.kotlin.android` plugin anywhere), Gradle 9.6.0 wrapper,
  JDK 17, compileSdk/targetSdk 37, minSdk 26.
- Version catalog (`gradle/libs.versions.toml`) as the single source of
  truth for all dependency versions.
- Eight convention plugins in `build-logic/convention`, including
  precompiled script plugins for application and feature modules
  (see ADR-0004).
- Locale-safe code generation: `-Duser.language=en -Duser.country=US` on
  the Gradle daemon — fixes Room 3 emitting Arabic-Indic digits in
  generated Kotlin on Arabic-locale machines.

#### Architecture (see `docs/adr/`)
- **ADR-0001** — Vertical feature modules with `api`/`impl` split; feature
  impls never depend on other impls (enforced by Gradle boundaries).
- **ADR-0002** — Room 3 (`androidx.room3:3.0.3`, new maven group) as the
  local single source of truth; schema JSON exported and committed.
- **ADR-0003** — Navigation 3 with an app-owned entry provider;
  composable-slot contracts (function-type properties) resolved via Hilt;
  `@Composable` signatures must not cross pure-JVM module boundaries.
- **ADR-0004** — Precompiled script plugins for the two highest-traffic
  conventions (application, feature).
- **ADR-0005** — Demo spaced-repetition scheduler (`demo-v1`) behind a
  `ReviewScheduler` interface, FSRS-ready via persisted scheduler state.

#### Core modules
- `core:model` — curriculum, progress, review and settings models with
  type-safe `@JvmInline` value-class identifiers.
- `core:content` — versioned JSON content schema, parser, structural
  validator and `ContentSource` abstraction (bundled now, downloadable
  packages later).
- `core:memory` — `ReviewScheduler` interface + `DemoReviewScheduler`
  with JSON-serializable state and version stamping.
- `core:database` — Room 3 database (5 entities, 5 DAOs, schema v1):
  `lesson_progress`, `activity_attempts`, `learning_events` (append-only),
  `review_cards`, `review_logs`.
- `core:datastore` — Preferences DataStore-backed user settings.
- `core:designsystem` — Spreva brand theme (light/dark/dynamic), tokens,
  German article color coding, shared components.
- `core:navigation` — shared Navigation 3 `NavKey`s.
- `core:common`, `core:logging`, `core:featureflags`, `core:testing`.

#### Domain layer
- `domain:curriculum` — repository contract + `ObserveCourse`,
  `GetLesson`, `GetNextLesson` use cases.
- `domain:learning` — `LearningRepository`, `CompleteLesson` use case
  (idempotent card provisioning + event logging).
- `domain:review` — `ReviewRepository`, `GetDueReviews`, `GradeReview`
  (scheduler-agnostic grading pipeline).

#### Features (api + impl)
- **Onboarding** — welcome → interface language → goal, persisted on
  completion.
- **Home** — Daily-Coach card, next lesson, live due-review count.
- **Course** — Learn tab (CEFR path) and level screen with per-lesson
  completion state.
- **Lesson** — player rendering five activity types (`text_intro`,
  `vocab_intro`, `multiple_choice`, `cloze`, `lesson_summary`), answer
  grading, attempt recording, progress persistence.
- **Review** — Practice tab + review session with reveal and
  Again/Hard/Good/Easy grading through the scheduler.
- **Settings** — theme (system/light/dark), dynamic color, live UI
  language switch via per-app locales (English/العربية).

#### App shell
- Single-activity Compose app with edge-to-edge, Window Size Class
  adaptive shell (`NavigationBar` on phones, `NavigationRail` on
  medium/expanded widths), predictive-back-ready `NavDisplay`.
- Offline-first DI wiring: bundled JSON content mirrored into app assets,
  Room-backed repositories, demo scheduler provider.
- RTL and Arabic localization (`values-ar`) across all feature modules.
- Adaptive launcher icon (adaptive + monochrome) with an abstract
  S/speech mark.
- `app-catalog` module for the future design lab.

#### Quality & CI
- 16 JVM unit tests (content parser/validator over the exact bundled
  bytes, demo scheduler determinism, `CompleteLesson` with fakes) —
  all passing.
- GitHub Actions CI (`.github/workflows/ci.yml`): `test` + `assembleDebug`
  on every PR and push to `main`, with test-report artifacts.
- `:app:lintDebug` passing (0 errors).
- Release build verified with R8 minification and resource shrinking.

### Verified on device
- Installed and launched on a physical device via adb: zero crashes,
  lesson screen rendered with live Arabic/German mixed content, and
  Room persistence confirmed through on-device sqlite inspection
  (`lesson_progress` row created on lesson open).
- Evidence: `docs/spreva_running.png`.

### Known limitations
- Instrumented test suites (`connectedAndroidTest`) are not yet authored;
  CI runs JVM tests only.
- Demo review scheduler uses fixed intervals (FSRS lands behind the same
  interface in a later phase).
- `app-catalog` is a shell; the component catalog ships with the
  screenshot-testing phase.

[Unreleased]: https://github.com/Alaa91H/SPREVA/compare/v0.1.0-alpha03...HEAD
[0.1.0-alpha03]: https://github.com/Alaa91H/SPREVA/releases/tag/v0.1.0-alpha03
