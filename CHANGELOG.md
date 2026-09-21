# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Learning intelligence
- Replaced the production demo interval scheduler with a **FSRS-6** implementation using the public 21-parameter default model at 90% desired retention. Stability, difficulty and last-review state are serialized in the existing opaque `schedulerState`, so no Room schema migration is required; legacy `demo-v1` cards migrate lazily on their next grade.
- Added FSRS interval previews to **Again / Hard / Good / Easy** before the learner commits a review rating.
- Added confidence-weighted **skill mastery** for grammar, reading, listening and pronunciation, plus completion-only evidence for writing and speaking until a real rubric evaluator exists. A Beta-style prior and evidence confidence prevent one lucky answer from becoming “100% mastery.”
- Added confidence-weighted **topic/lesson mastery**, repeated-error detection, repaired-error signals, rushed-guess detection, slow-recall detection and incomplete productive-task signals.
- Practice is now a **personalized learning dashboard**: due reviews, saved placement recommendation, skill mastery, and clickable focus lessons ranked from actual local attempt history.
- Repeated objective errors automatically create deterministic **FSRS remediation cards** after the second incorrect attempt. Vocabulary provisioning now correctly runs through the injected `CompleteLesson` use case; this also fixed an older lesson-player path that could bypass review-card provisioning and the completion event.
- Added an **adaptive A1–C1 placement diagnostic** sourced from the course’s original Exam & Skills Labs instead of a duplicate question bank. It starts at B1, tests four questions per visited level, moves up/down from evidence, persists the latest recommended starting level in Preferences DataStore, and explicitly does not claim CEFR certification.
- Learning Intelligence remains local-first: attempt history, mastery derivation, diagnostic state and review scheduling require no server-side learner profiling.

### Academic curriculum
- Added **Pronunciation & Prosody Labs** for A1–C1: 5 units / 20 lessons / 270 activities progressing from sound discrimination and basic stress to connected speech, pragmatic focus and C1 rhetorical prosody. `speaking_repeat` now supports local German TTS fallback when licensed native audio is absent, with multi-speed playback preserved.
- Added **Exam & Skills Labs** for A1, A2, B1, B2 and C1: 5 new units / 20 lessons / 280 activities covering reading, listening, dictation, free writing, free speaking, mixed mastery and original four-skill full mocks. Added renderer-native `dictation`, `free_write` and `speaking_prompt` activities, TTS fallback for listening, and 0.75×/1.0×/1.15× shadowing playback.
- Added **Integrated CEFR capstones** at A1, A2, B1, B2 and C1: 5 new units / 20 lessons / 270 activities that interleave grammar, vocabulary, real-life scenarios, error analysis and free production across multiple previously learned topics.
- Expanded every unit to a four-layer learning architecture: **Foundation → Real Life → Mastery → Casebook**. The new Mastery and Casebook layers add repeated retrieval, contrastive case drills, error diagnosis, transformation matrices, spaced/interleaved review, scenario transfer and level-appropriate revision.
- Replaced the two-lesson demo curriculum with a complete **A1 → C1 academic German path**: 83 units, 332 lessons and 3,817 rendered learning activities across everyday life, housing, health communication, work, public services, travel, finance, technology, media literacy, academic language, professional communication and advanced C1 discourse.
- Every lesson now ships indexed localized titles (German/Arabic/English), a deep explanation/context activity, contextual vocabulary, rule/example analysis, retrieval practice, cloze production, an active transfer/revision task and a stable Can-Do summary.
- Extended the content index schema with optional lesson titles and `activityCount`, and removed the repository's first-level-only lesson-summary assumption so all CEFR levels are first-class content.
- Added `scripts/validate_curriculum.py` and a CI curriculum-integrity gate covering level/package coverage, JSON/reference integrity, duplicate IDs, localized index fields, activity counts, MCQ/cloze validity, vocabulary blocks and Can-Do summaries.
- Added the full curriculum map and authoring/quality standard in `docs/CURRICULUM_A1_C1.md`.
- Content package version bumped from `2026.09.demo.2` to `2026.09.academic.6`.


### Added
- **Phase 4 stabilization — product-correctness pass (repository audit P0)**
  - **Onboarding bootstrap state**: the app shell now waits for the first real DataStore settings value (`AppBootstrapState.Loading/Ready`) and builds its start destination from persisted `onboardingComplete` — returning users land on Home, fresh installs on Welcome; the previous default-settings race sent everyone to onboarding.
  - **Learning goal persisted**: new `LearningGoal` enum (core:model) stored in DataStore; onboarding saves it on completion. Localized goal labels in English/Arabic replace raw ids (`start_from_zero` → “ابدأ من الصفر” / “Start from zero”).
  - **Safe lesson completion**: completion now runs through an explicit state machine (`LessonCompletionState.Idle/Saving/Success/Error`); navigation happens only on `Success`, so leaving the back stack can no longer cancel the progress/event/review-card writes mid-flight.
  - **Instruction-language policy**: `LocalizedText.resolveFor(UiLanguage)` — vocabulary translations follow the UI language (Arabic → ar→en→de, English → en→de) while German learning targets stay German regardless of chrome language.
  - **One-interaction microphone flow**: the permission launcher callback now starts recording immediately on grant; a denied grant shows the record button again instead of doing nothing.
  - **Safe recorder failures**: `VoiceRecorder.start` returns `RecordingStartResult.Started/Failed` — device-specific MediaRecorder exceptions surface as a friendly error instead of crashing the lesson.
  - **Media registry integrity**: real SHA-256 digests computed for all 16 bundled recordings (placeholder `fill-on-release-packaging` removed); source-level license wording made version-neutral (4.0/3.0 listed per file). New `MediaRegistryValidator` + 8 unit tests enforce: file existence, registry coverage of every distributed audio file, required metadata (license/licenseUrl/attribution/sourceUrl), digest match, duplicate rejection and the project's CC BY-SA-only media policy.
  - **CI**: Android `lint` joined the fast gate; media/content validation runs explicitly (`:core:content:test --rerun-tasks`); lint reports uploaded as artifacts.
  - Content package version bumped to `2026.09.demo.2`.
- **Phase 4.3 — shadowing starter (record + compare)**
  - New `speaking_repeat` activity: play the native recording, record yourself repeating it (runtime `RECORD_AUDIO` permission with pre-check), then compare by ear via playback of your own take.
  - `core:audio`: `VoiceRecorder` (MediaRecorder AAC), pure `AudioEnvelope` (RMS envelope from PCM WAV, stereo-fold, 32 buckets) and `WaveformComparator` (envelope similarity, unit-tested; activates when comparable envelopes exist — Ogg/AAC cross-decode is out of scope for this phase).
  - Privacy: recordings are temporary cache files deleted when the lesson is left; no network, no long-term retention (plan sections 101-108).
- **Unit 2 of A1 — Zahlen & Uhrzeit (numbers & time)**: 11 vocabulary items (eins…zehn + die Uhr) with native CC BY-SA recordings (joni, German Wiktionary + Kampy), a number MCQ, a minimal-pair listening drill (vier/acht/sechs), a cloze („Es ist acht Uhr“), and the first speaking-repeat activity; lesson 1 now has 7 activities.
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
