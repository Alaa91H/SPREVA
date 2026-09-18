# ADR-0006 — Media3 foundation + built-in TTS behind a `TtsProvider` contract

## Status
Accepted (Phase 4.1)

## Context
The blueprint (plan sections 31–34) requires learners to **hear German
words and sentences** in the lesson. Phase 4.1 is the audio foundation:
built-in TTS now; bundled course audio (Media3 playback) and ASR later.
The lesson renderer must not depend on engine-specific code, and the
provider layer must stay replaceable (same principle as ADR-0005's
scheduler-before-algorithm split).

## Decision
- New module **`core:audio`** (Android library + Hilt) holds all audio
  contracts and providers. Features depend on the module's interfaces,
  never on `android.speech.tts` or Media3 types.
- The Phase 4.1 contract is **`TtsProvider`**: `status: StateFlow<TtsStatus>`
  (`NOT_READY / READY / MISSING_GERMAN`), `speakGerman(article, text)`,
  `stop()`, `shutdown()`. `AndroidTtsProvider` implements it with Android's
  built-in `TextToSpeech` (`de-DE`, `QUEUE_FLUSH` so taps interrupt, not queue).
- Pure speech-text normalization lives in **`SpeechText`** (JVM-unit-tested):
  strips translation markers (`–`, `—`, `-`, `(`), trailing punctuation, and
  composes `article + noun` utterances.
- **Media3 1.11.1** (`media3-common`, `media3-exoplayer`, `media3-session`)
  is added to the catalog and `core:audio` now, so Phase 4.2 (bundled
  course audio) plugs into the same module without re-versioning.
- Lesson integration: header speaker button (repeats the German prompt of
  the current activity) + per-word speaker buttons on vocabulary cards.
  Buttons render only when `ttsStatus == READY`.

## Consequences
- No network, no external keys, no permissions required (platform TTS).
- German voice availability varies per device; the UI degrades silently
  (`MISSING_GERMAN`/`NOT_READY` hide the buttons instead of failing).
- The `Speaker` marker interface from the original plan is superseded by
  `TtsProvider` (testability + status reporting); documented here as the
  contract of record.
- Release builds keep R8: only used icon vectors survive from
  `material-icons-extended`.
