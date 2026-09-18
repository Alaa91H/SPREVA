# ADR-0003 — Navigation 3 with app-owned entry provider

## Status
Accepted (Phase 3, revised after on-device verification)

## Context
Navigation 3 (`androidx.navigation3:1.1.7`, stable) replaces the
navigation graph. Its stable API surface is:
`rememberNavBackStack(vararg NavKey)`, `NavBackStack` (a `StateObject`
implementing `MutableList`), the `entryProvider<T> { }` builder, and
`NavDisplay(backStack, onBack, entryProvider, …)`.

Two integration pitfalls were discovered and verified on a physical
device during bring-up:

1. **`@Composable` signatures cannot cross a pure-JVM module boundary.**
   Feature `api` modules were initially pure Kotlin/JVM with
   `fun interface …Entry { @Composable fun Content(...) }`. That
   compiles but emits a JVM signature without the Composer parameters;
   the Compose-compiled call sites in `impl`/`app` then fail at runtime
   with `NoSuchMethodError` on launch.
2. **`fun interface` + `@Composable` member is not a supported dispatch
   shape.** The reliable, officially supported pattern is a
   **composable function-type property**: `val content: @Composable (…)
   -> Unit`.

## Decision
- Feature `api` modules are **Android libraries with the Compose
  compiler** (`spreva.android.library` + `spreva.android.compose`), not
  pure-JVM modules.
- Each feature exposes a contract class holding a composable slot:
  `class LessonEntry(val content: @Composable (lessonId: String,
  onFinished: () -> Unit) -> Unit)`, provided by Hilt `@Provides` in the
  impl module and injected into the app's `EntryHolderViewModel`.
- `core:navigation` holds shared top-level `NavKey`s; app-internal keys
  (Welcome/Course/Lesson/ReviewSession) live in `app/navigation`.
- The app builds one `entryProvider<NavKey>` mapping keys to contract
  slots; feature impls never reference each other.
- Tab selection replaces the stack (`clear + add`); detail pushes append;
  lesson/review call `onBack` on finish.

## Consequences
- Type-safe, serializable keys; predictive-back-ready via NavDisplay.
- Contract changes are constructor/property changes — visible at compile
  time across api/impl/app.
- Launching a lesson renders and persists `lesson_progress` on a real
  device (verified 2026-09-18, physical device via adb).
- No Hilt-specific Nav3 integration exists yet — the holder pattern is
  the smallest stable bridge and can be replaced centrally later.
