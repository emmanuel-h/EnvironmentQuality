# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android native app showing drinking-water quality across French municipalities
(`fr.mandarine.environmentquality`). Kotlin, single `:app` module, AGP 9.x, Jetpack
Compose, min SDK 24, target SDK 36. `specs.md` is the product definition.

## Commands

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Lint (NewApi is fatal)
./gradlew lintDebug

# What CI runs on top, including the release build the test gate cannot see
./gradlew bundleRelease lintVitalRelease

# Assemble debug APK
./gradlew assembleDebug
```

CI (`.github/workflows/checks.yml`) runs unit tests, `lintDebug`, and `bundleRelease` +
`lintVitalRelease` on every push and pull request.

## Architecture

Three strict layers — a class must not import from a layer above it or import `android.*` in `domain/`:

```
app/src/main/java/fr/mandarine/environmentquality/
├── domain/       # Pure Kotlin: models (immutable data class), repository interfaces, use cases
├── data/         # Repository implementations (Hub'Eau, geo.api.gouv.fr, Room cache)
├── presentation/ # ViewModels + UI state sealed classes — depends on domain only
└── ui/           # Jetpack Compose. Activities call setContent and own no views.
```

**The app is entirely Compose.** There is no `res/layout/`, no `appcompat` and no Views
`material`. `res/values/` holds the window themes, `colors.xml` and `strings.xml`.

Test sources mirror the main layout under `app/src/test/java/fr/mandarine/environmentquality/`.

## Testing conventions

- Framework: JUnit4 (`@Test`, `@Before`, `@After`), Robolectric for anything touching Android
- Mocking: MockK (`mockk<T>()`, `every`, `coEvery`, `verify`, `coVerify`)
- Coroutines: `kotlinx-coroutines-test` (`runTest`)
- Test name pattern: `` fun `should <expected behaviour> when <condition>`() ``
