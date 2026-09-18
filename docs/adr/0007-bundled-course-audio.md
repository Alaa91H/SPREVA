# ADR-0007 — Bundled course audio via Media3, content-schema-driven

## Status
Accepted (Phase 4.2)

## Context
Phase 4.1 shipped built-in TTS behind `TtsProvider`. The master plan
(sections 32/34/83-85/99-100) requires **native-speaker audio** tied to
learning content: real recordings beat synthetic speech for a language
product, and audio must ship as content data, not UI code.

## Decision
- **Content schema**: `VocabularyItem` gains an optional `audio` field
  (path relative to `content/`). Parser maps it through; existing lesson
  files without audio stay valid (`ignoreUnknownKeys` + default `null`).
- **Player**: `core:audio` exposes `CourseAudioPlayer`
  (`play` / `playOrSpeak` / `stop`) implemented by
  `ExoPlayerCourseAudioPlayer` (Media3 1.11.1 — versioned and cached in
  Phase 4.1). `playOrSpeak` implements plan section 65's graceful
  degradation: bundled recording first, built-in TTS fallback when no
  recording exists.
- **Asset resolution**: `CourseAudioLocator` turns content paths into
  `asset:///…` URI strings via an injected opener (pure, JVM-tested —
  no `android.net.Uri` on the unit-test classpath). The app opener
  checks `assets.open()` existence before resolving; a later downloaded
  package source swaps the opener only, never the player or features.
- **Provenance**: bundled recordings are Wikimedia Commons **CC BY-SA 4.0**
  (native speaker Jeuwre, Berlin) with `content/audio/licenses.json`
  registry per plan sections 99-100. GFDL / CC BY-NC-SA candidates were
  deliberately excluded (non-commercial / copyleft-stack mismatch).
- Lesson wiring: `LessonViewModel.speakWord(article, german, audioPath)`
  delegates to `CourseAudioPlayer`; vocabulary speaker buttons pass
  `word.audio` through.

## Consequences
- Lesson 1 vocabulary plays real native German audio offline; the
  "Wie heißen Sie?" item intentionally has no recording yet and
  demonstrates the TTS fallback live.
- Attribution ships inside the app content package; sha256 checksums
  are filled at release packaging (plan section 97).
- Audio focus handling stays minimal for Phase 4.2 (short clips in an
  active lesson); a session/audio-focus policy lands with longer media.
