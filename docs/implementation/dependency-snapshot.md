# Dependency Snapshot — 18 September 2026

Verified against official release channels (Google Maven metadata,
kotlinlang.org, developer.android.com release notes) on 2026-09-18,
per the blueprint's Version Verification Gate (plan section 2/386).

## Toolchain

| Component     | Version  | Source                                              |
|---------------|----------|-----------------------------------------------------|
| Kotlin        | 2.4.20   | kotlinlang.org/docs/releases.html (7 Sep 2026)      |
| AGP           | 9.4.0    | developer.android.com/build/releases/gradle-plugin  |
| Gradle        | 9.6.0    | wrapper (`gradle/wrapper/gradle-wrapper.properties`)|
| JDK           | 17       | Microsoft OpenJDK 17.0.20 (local), Temurin 17 in CI |
| compileSdk    | 37       | Android 17 (plan section 2)                         |
| targetSdk     | 37       |                                                     |
| minSdk        | 26       |                                                     |

## AndroidX / Google

| Artifact                                | Version | Notes                                   |
|-----------------------------------------|---------|-----------------------------------------|
| androidx.compose:compose-bom            | 2026.09.00 | pulls compose-ui 1.12.1, material3 1.4.0 |
| androidx.compose.material3:material3-window-size-class | 1.4.0 | pinned explicitly (not in BOM) |
| androidx.navigation3:navigation3-runtime| 1.1.7   | stable                                  |
| androidx.navigation3:navigation3-ui     | 1.1.7   | stable                                  |
| androidx.room3:room3-runtime            | 3.0.3   | **new group androidx.room3** (ADR-0002) |
| androidx.room3:room3-compiler           | 3.0.3   | KSP                                     |
| androidx.datastore:datastore-preferences| 1.2.1   | stable                                  |
| androidx.lifecycle:*                    | 2.11.0  | runtime-compose + viewmodel-compose     |
| androidx.activity:activity-compose      | 1.13.0  | stable                                  |
| androidx.appcompat:appcompat            | 1.7.1   | per-app locales (MainActivity)          |
| androidx.hilt:hilt-navigation-compose   | 1.4.0   | stable                                  |
| com.google.dagger:hilt-android          | 2.60.1  | stable                                  |
| com.google.devtools.ksp                 | 2.3.12  | matches Kotlin 2.4.20                   |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.9.0 | stable                        |
| org.jetbrains.kotlinx:kotlinx-coroutines| 1.10.2  | stable                                  |
| javax.inject:javax.inject               | 1       | @Inject in pure-JVM domain modules      |

## Testing

| Artifact                | Version |
|-------------------------|---------|
| junit:junit             | 4.13.2  |
| org.jetbrains.kotlinx:kotlinx-coroutines-test | 1.10.2 |
| app.cash.turbine        | 1.2.1   |

## Deviations from the blueprint (section 386)

1. **Room 3 coordinates** — the blueprint lists "Room 3 stable 3.0.3";
   the actual artifact coordinates are `androidx.room3:room3-*` (new
   maven group + `androidx.room3.*` package), not `androidx.room:3.0.3`.
   The legacy `androidx.room:room-compiler:2.8.5` produced code that did
   not compile under Kotlin 2.4.20 in this toolchain.
2. **Room transactions** — Room 3 has no `-ktx` artifact; use
   `androidx.room3.withWriteTransaction` / `withReadTransaction`.
3. **material3-window-size-class** — not part of the compose-bom;
   pinned at 1.4.0 explicitly.
4. **AGP 9 built-in Kotlin** — no `org.jetbrains.kotlin.android` plugin
   anywhere; Android modules get Kotlin via AGP 9 natively.

## Known environment notes

- **Arabic-locale machines**: Room 3's compiler formats numbers with
  `String.format`, which emits Arabic-Indic digits (`١٢٣`) on
  Arabic-locale JVMs and breaks generated Kotlin. `gradle.properties`
  forces `-Duser.language=en -Duser.country=US` on the Gradle daemon.
  If generation still fails after locale changes, delete the module's
  `build/` directory (stale generated files survive daemon restarts).
