# Contris

Native Android app that shows information about every country in the world, powered by the
[REST Countries API v5](https://restcountries.com/docs/countries).

Jetpack Compose · Material 3 · Navigation 3 · Hilt · Room · Retrofit/OkHttp · kotlinx.serialization · Coil.
Architecture and feature specs live in [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md).

## Features

- **Countries** – searchable list (names, native names, alternates, capitals, ISO codes), filters
  (region, subregion, continent, landlocked, UN membership, sovereignty, organisations) and sorting.
- **Country detail** – flag, overview, geography with tappable borders, people & culture, economy
  (Gini history), memberships, codes, external links and share.
- **Favorites** – swipe to remove with undo.
- **Compare** – side-by-side comparison of two countries with the larger value highlighted.
- **Quiz** – flag → country, country → capital, capital → country; 10 questions, score and streak history.
- **Settings** – theme, dynamic colour, metric/imperial units, manual refresh, reset actions.

The app is **offline-first**: the whole dataset (≈254 records, 3 requests) is synced into Room and
every screen reads from the database. Data is refreshed automatically when older than 72 hours,
or manually from Settings / pull-to-refresh (debounced to once per minute).

## Setup

1. Get a REST Countries API key (free plan: 1,000 requests / month).
2. Add it to `local.properties` (this file is git-ignored):

   ```properties
   REST_COUNTRIES_API_KEY=rc_live_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```

   The key is exposed to the app as `BuildConfig.REST_COUNTRIES_API_KEY` and sent as a
   `Authorization: Bearer …` header. Release builds fail fast if it is missing.

3. No JDK is required on `PATH`; use Android Studio's bundled runtime for CLI builds:

   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ```

## Build & test

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew :app:connectedDebugAndroidTest
```

Unit tests cover mappers, the SQL query builder, the Retrofit layer (MockWebServer), the Room DAOs
(Robolectric), the sync manager (staleness, paging, retries, debounce, mutex), the quiz generator and
comparison use cases, and every ViewModel. Instrumented tests cover the key Compose screens and a
Hilt smoke test that boots `MainActivity` against a MockWebServer. **Tests never call the live API.**

## Project layout

```
app/src/main/java/com/example/contris
├── ContrisApplication.kt / MainActivity.kt
├── di/          Hilt modules (app, network, database, repositories)
├── data/        remote (Retrofit + DTOs) · local (Room, DataStore) · mapper · repository · sync
├── domain/      models · repository interfaces · use cases (pure Kotlin)
└── ui/          navigation (Nav3 back stacks) · theme · common components · one package per feature
```

Each feature follows MVVM + unidirectional data flow: immutable `UiState`, `UiEvent` intents and
one-shot `UiEffect`s, with a stateless `XScreen(state, onEvent)` composable and a thin `XRoute`.

## Notes

- Coil is pinned to 3.4.0: newer releases are compiled with Kotlin 2.4, which AGP 9.4's bundled
  Kotlin 2.2 compiler cannot read.
- `android.disallowKotlinSourceSets=false` is set because the KSP plugin still registers generated
  sources through the Kotlin source-set DSL.
- Room schemas are exported to `app/schemas/` for migration tests.
