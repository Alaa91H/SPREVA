# SPREVA Learning Intelligence

SPREVA's Learning Intelligence layer turns local learning evidence into three distinct decisions:

1. **When should this item return?** → FSRS-6 memory scheduling.
2. **What should the learner focus on?** → mastery + mistake intelligence.
3. **Where should a new learner start?** → adaptive placement.

These responsibilities are deliberately separate. A memory scheduler is not a proficiency evaluator, and a short placement diagnostic is not a CEFR certificate.

## 1. FSRS-6 memory engine

Production review scheduling is implemented behind `ReviewScheduler` by `Fsrs6ReviewScheduler`.

### Model

The implementation uses the public FSRS-6 default 21-parameter vector:

```text
0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194,
0.001, 1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629,
1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542
```

The default desired retention is **0.90**. Per-card state stores:

- **stabilityDays** — the interval at which the model predicts approximately 90% retrievability;
- **difficulty** — bounded to 1…10;
- **lastReviewAtEpochMs** — required to compute elapsed time and retrievability.

Reference algorithm: Open Spaced Repetition, FSRS algorithm documentation:
https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm

### Persistence and migration

No new Room columns are required. The existing `ReviewCard.schedulerState` JSON stores the scheduler-specific state, and `schedulerVersion` is now:

```text
fsrs-6-default-v1
```

Cards produced by the legacy `demo-v1` scheduler are migrated lazily: the next grade initializes an FSRS state and persists it with the new version. This avoids destructive migration of review history.

### Transparent review UI

Before the learner selects **Again / Hard / Good / Easy**, the UI previews the next interval produced by the same scheduler that will persist the grade. The preview is read-only and never mutates the card.

## 2. Review material

### Vocabulary cards

Completing a lesson now always flows through the injectable `CompleteLesson` use case. It:

1. persists lesson completion;
2. provisions deterministic vocabulary cards;
3. records the `LessonCompleted` event.

This also repairs the former lesson-player path that completed progress directly and could bypass card provisioning.

### Repeated-error remediation

An isolated mistake is not automatically turned into a flashcard. After **two incorrect attempts on the same objective activity**, SPREVA creates one deterministic remediation card with `INSERT OR IGNORE`.

Supported remediation sources:

- multiple choice → question + correct option;
- cloze → sentence pattern + accepted answer;
- listening choice → bundled audio recall when real licensed audio is available;
- dictation → bundled audio recall when available.

TTS-only listening tasks are deliberately not converted into audio review cards because review cards currently store a bundled media path, not hidden fallback text.

## 3. Mastery evidence

Mastery is derived from Room's append-only `activity_attempts`; there is no second mutable “mastery table” that can drift away from the evidence.

### Skill areas

- Grammar
- Reading
- Listening
- Writing
- Speaking
- Pronunciation

Objective evidence currently comes from:

- `multiple_choice`
- `cloze`
- `listening_choice`
- `dictation`

Writing and free speaking are **completion evidence only**. Reaching a word count or recording duration does not prove linguistic quality.

### Confidence weighting

For objective evidence, SPREVA uses a Beta(2,2)-style prior and an evidence-confidence factor. Confidence grows toward full weight over roughly twelve attempts. The displayed mastery score is shrunk toward 50% while evidence is sparse.

This design prevents:

- one correct answer → “100% mastery”;
- one mistake → “0% mastery”;
- writing length → false writing-quality score.

The same evidence model produces both:

- **skill mastery** (for example listening); and
- **topic/lesson mastery** (for example one specific grammar lesson).

These are learning signals, not standardized test scores.

## 4. Mistake intelligence

The engine detects actionable patterns from attempt history:

| Pattern | Current signal |
|---|---|
| Repeated error | at least two incorrect attempts on one activity |
| Repaired after error | one or more errors followed by a correct latest attempt |
| Possible rushed guess | incorrect response in ≤ 1.5 seconds |
| Slow retrieval | average correct objective response > 45 seconds |
| Productive incomplete | writing/speaking task did not reach its completion threshold |

Severity combines error rate, amount of evidence and pattern-specific weight. The highest-priority lesson signals become the Practice dashboard's **Recommended focus** list.

The user can tap a focus recommendation and reopen the exact lesson that generated the evidence.

## 5. Adaptive placement

The diagnostic reuses original questions already present in each level's **Exam & Skills Lab**; SPREVA does not maintain a hidden duplicate question bank.

### Flow

- Start at **B1**.
- Test **4 objective questions** at the current level.
- **3–4 correct** → test one level higher.
- **0–1 correct** → test one level lower.
- **2 correct** → settle at the current level.
- If the learner passes a level, moves upward, then clearly fails the harder level, recommend the previously demonstrated level.
- Boundaries are A1 and C1.

The latest recommendation is saved in Preferences DataStore and shown on the Practice dashboard.

### Interpretation

The result is explicitly a **recommended starting level**, not:

- a CEFR certificate;
- a Goethe-Institut or telc result;
- proof of productive speaking/writing ability.

The learner's longer-term mastery profile can refine daily focus after real lesson/review evidence accumulates.

## 6. Privacy and offline-first behavior

All current intelligence uses local data:

- Room attempt history;
- Room review cards/logs;
- bundled curriculum content;
- Preferences DataStore for the latest placement recommendation.

No learner attempt history needs to leave the device for FSRS scheduling, mastery calculation, focus recommendations or adaptive placement.

## 7. Tests and quality gates

The phase adds deterministic JVM tests for:

- FSRS rating interval ordering;
- stability growth after successful delayed recall;
- lapses and lazy migration from `demo-v1`;
- the FSRS forgetting-curve invariant R(S) ≈ 0.90;
- mastery confidence weighting;
- repeated-error focus generation;
- non-scoring of productive completion as linguistic quality;
- placement movement up/down and B1→B2→C1 paths.

Existing CI continues to run:

- curriculum integrity;
- JVM unit tests;
- Android lint;
- `assembleDebug`;
- media/content validation.

## 8. Known limitations and next calibration work

The foundation is intentionally conservative:

- FSRS uses the public default parameters; learner-specific parameter optimization should wait until enough real review history exists.
- Placement uses a short adaptive bank and is a starting-point diagnostic, not a validated standardized assessment.
- Writing and speaking need human/expert-calibrated rubrics or a validated evaluator before SPREVA should report quality scores.
- Skill classification currently derives semantics from activity type and curriculum unit context; future content schema versions can add explicit skill/tag metadata.
- Remediation audio cards require bundled licensed audio; TTS fallback review cards can be added later with an explicit fallback-text field in the review-card model.
