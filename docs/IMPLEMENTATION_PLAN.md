# Contris — Implementation Plan

Native Android app that shows information about every country in the world, backed by the
**REST Countries API v5**. Built with Jetpack Compose, a layered **MVVM + Unidirectional Data Flow**
architecture, offline-first storage in Room, and Hilt for dependency injection.

Date: 2026-10-07 · Status: approved scope, ready to implement.

---

## 1. Scope & decisions

| Topic | Decision |
|---|---|
| Features | Countries list · Search · Filters & sort · Country detail · Bordering countries · **Favorites** · **Compare (2 countries)** · **Quiz** (flag→country, country→capital, capital→country) · **Settings** |
| Top-level tabs | Countries · Favorites · Quiz · Compare · Settings |
| Data strategy | **Offline-first.** Full dataset (254 records, 3 requests) synced into Room; every screen reads from the DB. Auto-refresh when data is older than 72 h; manual refresh from Settings and pull-to-refresh. |
| Architecture | Layered: `ui` → `domain` → `data`. MVVM + UDF (immutable `UiState`, `UiEvent` intents, one-shot `UiEffect`s). |
| DI | Hilt (KSP) |
| Network / JSON | Retrofit 3 + OkHttp 5 + kotlinx.serialization |
| Navigation | Navigation 3 (`NavDisplay`, `NavKey` back stacks owned by the app) with per-tab back stacks |
| Project structure | Single `:app` module, layered packages |
| API key | `local.properties` → `BuildConfig.REST_COUNTRIES_API_KEY` (never committed) |
| Language | English UI (strings.xml) + English data; `names.translations` is not synced |
| Testing | Unit tests (ViewModels, repository, mappers, DAO) + Compose UI tests for key screens |
| Min / target SDK | 28 / 37 (already configured) |

---

## 2. API contract (REST Countries v5)

Base URL `https://api.restcountries.com/countries/v5` — GET only.
Auth header: `Authorization: Bearer <key>`. Never pass the key as a query param.

### 2.1 Quota and rate limits (free plan)

| Limit | Value | Consequence for the app |
|---|---|---|
| Requests / month | **1,000** | Only the sync job calls the API. All search/filter/detail work is local. |
| Max `limit` per page | 100 | Full sync = `ceil(254 / 100)` = **3 requests**. |
| Throughput | 20 requests / 10 s → `429` | Sync pages are fetched sequentially; a manual refresh is debounced (min 60 s between attempts). |
| Caching allowed | up to 3 days | `STALE_AFTER = 72h`. |
| Over quota | `403` after a grace period | Surface "Quota exhausted — showing cached data" and keep the DB. |

Monthly budget at 3 requests/sync: even a refresh every day is 90 requests/month.

### 2.2 The only endpoint the app calls

```
GET /countries/v5
  ?limit=100
  &offset={0|100|200}
  &response_fields_omit=names.translations,flag.colors.palette,assets,leaders
```

- `leaders` is a paid-only field; on the free plan it is replaced by a notice object. Omitting it keeps the
  payload homogeneous. `assets` and `flag.colors.palette` are heavy and unused.
- Ordering is stable (ascending `names.common`), so offset pagination is safe.
- Stop when `meta.more == false` (defensive upper bound: 10 pages).

### 2.3 Response envelope

```jsonc
{ "data": { "objects": [ /* country */ ], "meta": { "total": 254, "count": 100, "limit": 100, "offset": 0, "more": true, "request_id": "…" } } }
// errors
{ "errors": [ { "message": "…" } ] }
```

Status codes to handle: `200`, `400` (bug in our request), `401` (bad key), `403` (frozen / quota), `429` (rate limit — retry with backoff ≥ 10 s, max 2 retries), `5xx` (retry once), no network.

### 2.4 Country object (fields the app uses)

Verified against a live response. Note that non-ISO entries exist (e.g. Abkhazia with `alpha_2 = ""`), so
**`uuid` is the primary key**, not the alpha codes.

| Path | Type | Used for |
|---|---|---|
| `uuid` | string | Primary key |
| `names.common`, `names.official` | string | List, detail, search |
| `names.alternates` | string[] | Search |
| `names.native` | map<iso639-3, {common, official}> | Detail, search |
| `codes.alpha_2`, `alpha_3`, `ccn3`, `cioc`, `fifa`, `fips`, `gec` | string (may be `""`) | Detail, search, border lookup (`alpha_3`) |
| `capitals[]` | `{name, coordinates{lat,lng}, attributes{primary,…}}` | List subtitle, detail, quiz |
| `flag.emoji`, `flag.url_png`, `flag.url_svg`, `flag.description` | string | List, detail, quiz |
| `flag.colors.dominant`, `prominent`, `swatches.{vibrant,muted,…}` | hex string / null | Detail header tint |
| `region`, `subregion`, `continents[]` | string | Filters, detail |
| `landlocked` | boolean | Filter, detail |
| `borders[]` | alpha_3[] | Detail chips |
| `area.kilometers`, `area.miles` | number | Detail, compare, sort, units setting |
| `coordinates.lat/lng` | number | Detail (map link) |
| `population` | number | List/sort, detail, compare |
| `currencies[]` | `{code, name, symbol}` | Detail, compare |
| `languages[]` | `{name, native_name, bcp47, iso639_3,…}` | Detail, compare |
| `calling_codes[]`, `tlds[]`, `timezones[]` | string[] | Detail |
| `cars.driving_side`, `cars.signs[]` | string | Detail, compare |
| `postal_code.format` | string | Detail |
| `date.start_of_week` | string | Detail |
| `economy.gini_coefficient` | map<year, number> | Detail (latest), compare |
| `government_type` | string | Detail |
| `classification.{sovereign, un_member, un_observer, dependency, dependency_type, disputed, iso_status}` | boolean / string | Filters, detail |
| `memberships.{un, eu, eurozone, schengen, nato, commonwealth, oecd, g7, g20, brics, opec, african_union, asean, arab_league}` | boolean | Filters, detail chips, compare |
| `descriptions.short`, `descriptions.long` | string | Detail |
| `links.{wikipedia, official, google_maps, open_street_maps}` | string | Detail actions |
| `units.measurement_system` | string | Detail |
| `_meta.lastUpdatedTimestamp` | epoch seconds | Detail footer |

Parser config: `Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }` — every DTO field is nullable or defaulted so a schema addition never crashes the sync.

---

## 3. Architecture

### 3.1 Layers

```
ui/        Compose screens, ViewModels, UiState/UiEvent/UiEffect, navigation, theme
domain/    Pure Kotlin: models, repository interfaces, use cases
data/      Retrofit API + DTOs, Room entities/DAOs, DataStore, mappers, repository impls, sync
di/        Hilt modules
```

Dependency rule: `ui → domain ← data`. The `ui` layer never sees DTOs or entities; `domain` has no Android imports.

### 3.2 UDF contract (applies to every feature)

```kotlin
// Immutable state — the single source of truth for a screen
data class XUiState(...)                       // or a sealed interface when phases are exclusive

// User intents — the only way the UI talks to the ViewModel
sealed interface XUiEvent { data object Refresh : XUiEvent; data class QueryChanged(val q: String) : XUiEvent }

// One-shot side effects consumed exactly once (navigation, snackbars)
sealed interface XUiEffect { data class ShowMessage(val text: UiText) : XUiEffect }

@HiltViewModel
class XViewModel @Inject constructor(...) : ViewModel() {
    val uiState: StateFlow<XUiState>           // stateIn(viewModelScope, WhileSubscribed(5_000), initial)
    val effects: Flow<XUiEffect>               // Channel(BUFFERED).receiveAsFlow()
    fun onEvent(event: XUiEvent)
}

// Composables
@Composable fun XRoute(vm: XViewModel = hiltViewModel(), onNavigate: ...)   // collects state, forwards events
@Composable fun XScreen(state: XUiState, onEvent: (XUiEvent) -> Unit)        // stateless, previewable, testable
```

Rules:
- State flows down, events flow up. No mutable state leaks out of ViewModels (`StateFlow`, never `MutableStateFlow`).
- Derived state is computed in the ViewModel (`combine`/`map`), not in composables.
- Navigation is requested via `UiEffect`; the `Route` composable performs it using the back stack.
- Long-running work lives in repositories/use cases, dispatched on `Dispatchers.IO` through an injected `DispatcherProvider` (testable).
- Errors are modelled as data (`error: UiText?` / `Result`), not exceptions crossing layers.

### 3.3 Package layout (`com.example.contris`)

```
ContrisApplication.kt                 @HiltAndroidApp
MainActivity.kt                       @AndroidEntryPoint, sets ContrisApp()

di/   AppModule, NetworkModule, DatabaseModule, RepositoryModule, DispatcherModule

data/
  remote/   RestCountriesApi.kt, AuthInterceptor.kt, dto/ (CountriesPageDto, CountryDto, ...)
  local/    ContrisDatabase.kt, Converters.kt, entity/ (CountryEntity, FavoriteEntity, QuizResultEntity),
            dao/ (CountryDao, FavoriteDao, QuizResultDao), prefs/ (SettingsDataStore)
  mapper/   CountryDtoToEntity.kt, CountryEntityToDomain.kt
  repository/ CountryRepositoryImpl, FavoritesRepositoryImpl, QuizRepositoryImpl, SettingsRepositoryImpl
  sync/     CountrySyncManager.kt

domain/
  model/    Country, CountrySummary, Capital, Currency, Language, Memberships, Classification,
            FlagInfo, CountryFilters, CountrySort, SyncStatus, UnitSystem, ThemeMode, QuizMode,
            QuizQuestion, QuizRound, QuizResult, Comparison
  repository/ CountryRepository, FavoritesRepository, QuizRepository, SettingsRepository
  usecase/  SyncCountriesUseCase, ObserveCountriesUseCase, ObserveCountryUseCase,
            ObserveFavoritesUseCase, ToggleFavoriteUseCase, GenerateQuizRoundUseCase,
            SaveQuizResultUseCase, BuildComparisonUseCase

ui/
  ContrisApp.kt                      NavigationSuiteScaffold + NavDisplay
  navigation/ NavKeys.kt, TopLevelBackStack.kt, EntryProviders.kt
  theme/     (existing) + FlagTint.kt
  common/    UiText.kt, formatters (NumberFormatters.kt), components/ (CountryListItem, FlagImage,
             StatRow, ChipRow, ErrorState, EmptyState, LoadingState, SearchBar)
  countries/ CountriesRoute.kt, CountriesScreen.kt, CountriesViewModel.kt, CountriesUiState.kt, FilterSheet.kt
  detail/    CountryDetailRoute/Screen/ViewModel/UiState, sections/ (OverviewSection, GeographySection, ...)
  favorites/ FavoritesRoute/Screen/ViewModel/UiState
  compare/   CompareRoute/Screen/ViewModel/UiState, CountryPickerSheet.kt
  quiz/      QuizHomeRoute/Screen, QuizPlayRoute/Screen, QuizResultScreen, QuizViewModel, QuizUiState
  settings/  SettingsRoute/Screen/ViewModel/UiState
```

---

## 4. Tech stack & versions

Verified against Google Maven / Maven Central on 2026-10-07. Compatible with the project's AGP 9.4.1,
Kotlin 2.2.10 (AGP built-in Kotlin), Gradle 9.6.

### 4.1 `gradle/libs.versions.toml` additions

```toml
[versions]
agp = "9.4.1"                 # existing
kotlin = "2.2.10"             # existing
ksp = "2.2.10-2.0.2"
composeBom = "2026.09.00"     # bump from 2025.12.00
coreKtx = "1.19.1"            # bump
activityCompose = "1.13.0"    # bump
lifecycle = "2.11.0"          # replaces lifecycleRuntimeKtx 2.6.1
navigation3 = "1.2.0"
hilt = "2.60.1"
hiltNavigationCompose = "1.4.0"
room = "2.8.5"
datastore = "1.2.1"
retrofit = "3.0.0"
okhttp = "5.5.0"
kotlinxSerialization = "1.11.0"
coroutines = "1.11.0"
coil = "3.6.3"
material3Adaptive = "1.3.0"
# test
junit = "4.13.2"
androidxJunit = "1.3.0"
espressoCore = "3.7.0"
turbine = "1.2.1"
mockk = "1.14.11"
robolectric = "4.17"
truth = "1.4.5"

[libraries]
androidx-lifecycle-runtime-compose      = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose    = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-navigation3 = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui      = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-compose-material3-adaptive = { group = "androidx.compose.material3.adaptive", name = "adaptive", version.ref = "material3Adaptive" }
hilt-android  = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-room-runtime  = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx      = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing  = { group = "androidx.room", name = "room-testing", version.ref = "room" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
okhttp-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test    = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
coil-compose       = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
coil-svg           = { group = "io.coil-kt.coil3", name = "coil-svg", version.ref = "coil" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
mockk   = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
room = { id = "androidx.room", version.ref = "room" }
```

### 4.2 `app/build.gradle.kts` changes

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

import java.util.Properties
val localProps = Properties().apply {
    val f = rootProject.file("local.properties"); if (f.exists()) f.inputStream().use(::load)
}

android {
    defaultConfig {
        buildConfigField("String", "REST_COUNTRIES_API_KEY", "\"${localProps.getProperty("REST_COUNTRIES_API_KEY", "")}\"")
        buildConfigField("String", "REST_COUNTRIES_BASE_URL", "\"https://api.restcountries.com/countries/v5/\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
room { schemaDirectory("$projectDir/schemas") }   // exported schemas, committed for migration tests
```

`local.properties` (gitignored) gets one new line: `REST_COUNTRIES_API_KEY=<your key>`.
The build fails fast with a clear message if the key is blank in a release build.

### 4.3 Toolchain note

No JDK is on `PATH`; use Android Studio's bundled JBR for CLI builds:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

---

## 5. Data layer specification

### 5.1 Network

```kotlin
interface RestCountriesApi {
    @GET(".")                       // base URL already ends in /v5/
    suspend fun getCountries(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int,
        @Query("response_fields_omit") omit: String = OMITTED_FIELDS,
    ): CountriesPageDto
}
const val OMITTED_FIELDS = "names.translations,flag.colors.palette,assets,leaders"
```

- `AuthInterceptor` adds `Authorization: Bearer ${BuildConfig.REST_COUNTRIES_API_KEY}`.
- `HttpLoggingInterceptor` at `BASIC` level, debug builds only, with `redactHeader("Authorization")`.
- OkHttp timeouts: connect 15 s, read 30 s. OkHttp disk cache 10 MB (helps if the API sends cache headers).
- Errors are mapped in the repository to a sealed `SyncError { Network, Unauthorized, QuotaExceeded, RateLimited, Server(code), Unknown }`.

DTOs (`@Serializable`, all fields nullable/defaulted): `CountriesPageDto(data: PageDataDto?)`,
`PageDataDto(objects: List<CountryDto> = emptyList(), meta: MetaDto?)`, `MetaDto(total, count, limit, offset, more)`,
`CountryDto` mirroring §2.4 with nested `NamesDto, NativeNameDto, CodesDto, CapitalDto, CoordinatesDto, FlagDto,
FlagColorsDto, SwatchesDto, AreaDto, CurrencyDto, LanguageDto, CarsDto, PostalCodeDto, DateDto, EconomyDto,
ClassificationDto, MembershipsDto, DescriptionsDto, LinksDto, UnitsDto, RecordMetaDto`.

### 5.2 Room schema (`contris.db`, version 1)

**`countries`** — one row per country; scalars as columns (filterable/sortable), structured data as JSON text via `Converters` (kotlinx.serialization).

| Column | Type | Notes |
|---|---|---|
| `uuid` | TEXT PK | |
| `name_common`, `name_official` | TEXT NOT NULL | index on `name_common` |
| `alpha2`, `alpha3`, `ccn3`, `cioc`, `fifa` | TEXT | index on `alpha3` (border lookups) |
| `search_text` | TEXT NOT NULL | lower-cased `common|official|alternates|native names|alpha2|alpha3` — searched with `LIKE '%q%'` |
| `capital_name` | TEXT | primary capital (first with `attributes.primary`, else first) |
| `capital_lat`, `capital_lng` | REAL | |
| `flag_emoji`, `flag_png`, `flag_svg`, `flag_description` | TEXT | |
| `flag_dominant`, `flag_prominent`, `flag_vibrant`, `flag_muted` | TEXT | hex or null |
| `region`, `subregion` | TEXT | indexed |
| `continents` | TEXT (JSON array) | |
| `landlocked` | INTEGER (bool) | |
| `borders` | TEXT (JSON array of alpha3) | |
| `area_km2`, `area_mi2` | REAL | |
| `lat`, `lng` | REAL | |
| `population` | INTEGER | |
| `currencies`, `languages`, `calling_codes`, `tlds`, `timezones`, `car_signs` | TEXT (JSON) | |
| `driving_side`, `postal_format`, `start_of_week`, `government_type`, `measurement_system` | TEXT | |
| `gini_latest_year`, `gini_latest` | INTEGER / REAL | derived at mapping time; full map also stored as `gini_json` |
| `sovereign`, `un_member`, `un_observer`, `dependency`, `disputed` | INTEGER (bool) | |
| `dependency_type`, `iso_status` | TEXT | |
| `m_un`, `m_eu`, `m_eurozone`, `m_schengen`, `m_nato`, `m_commonwealth`, `m_oecd`, `m_g7`, `m_g20`, `m_brics`, `m_opec`, `m_african_union`, `m_asean`, `m_arab_league` | INTEGER (bool) | flat for filtering |
| `description_short`, `description_long` | TEXT | |
| `link_wikipedia`, `link_official`, `link_google_maps`, `link_osm` | TEXT | |
| `last_updated_epoch` | INTEGER | from `_meta` |

**`favorites`**: `country_uuid TEXT PK` (FK → countries, cascade delete), `added_at INTEGER`.

**`quiz_results`**: `id INTEGER PK autoincrement`, `mode TEXT`, `score INTEGER`, `total INTEGER`, `best_streak INTEGER`, `played_at INTEGER`.

**DataStore (`settings`)**: `unit_system` (`metric|imperial`, default metric), `theme_mode` (`system|light|dark`), `dynamic_color` (bool, default true), `last_sync_epoch_ms` (long), `last_sync_total` (int).

### 5.3 DAOs

```kotlin
@Dao interface CountryDao {
    @RawQuery(observedEntities = [CountryEntity::class])
    fun observeSummaries(query: SupportSQLiteQuery): Flow<List<CountrySummaryRow>>   // built by CountryQueryBuilder
    @Query("SELECT * FROM countries WHERE uuid = :uuid") fun observeByUuid(uuid: String): Flow<CountryEntity?>
    @Query("SELECT * FROM countries WHERE alpha3 IN (:codes)") suspend fun getByAlpha3(codes: List<String>): List<CountryEntity>
    @Query("SELECT COUNT(*) FROM countries") suspend fun count(): Int
    @Query("SELECT DISTINCT region FROM countries WHERE region != '' ORDER BY region") fun observeRegions(): Flow<List<String>>
    @Query("SELECT DISTINCT subregion FROM countries WHERE subregion != '' ORDER BY subregion") fun observeSubregions(): Flow<List<String>>
    @Query("SELECT * FROM countries WHERE flag_png IS NOT NULL AND capital_name IS NOT NULL AND sovereign = 1") suspend fun getQuizPool(): List<CountryEntity>
    @Upsert suspend fun upsertAll(items: List<CountryEntity>)
    @Query("DELETE FROM countries WHERE uuid NOT IN (:keep)") suspend fun deleteNotIn(keep: List<String>)
    @Transaction suspend fun replaceAll(items: List<CountryEntity>) { upsertAll(items); deleteNotIn(items.map { it.uuid }) }
}
```

`CountryQueryBuilder(filters, sort, query)` produces the `SELECT uuid, name_common, capital_name, region, flag_emoji, flag_png, population, area_km2 FROM countries WHERE … ORDER BY …` statement with bound args (no string interpolation of user input).

`FavoriteDao`: `observeAll(): Flow<List<FavoriteEntity>>`, `observeIsFavorite(uuid): Flow<Boolean>`, `insert`, `delete`, `clear`,
plus a `@Query` joining favorites→countries for the Favorites list.
`QuizResultDao`: `insert`, `observeRecent(limit = 20)`, `observeBest(mode)`, `clear`.

### 5.4 Repositories (interfaces in `domain`, impls in `data`)

```kotlin
interface CountryRepository {
    fun observeCountries(query: String, filters: CountryFilters, sort: CountrySort): Flow<List<CountrySummary>>
    fun observeCountry(uuid: String): Flow<Country?>
    suspend fun getByAlpha3(codes: List<String>): List<CountrySummary>
    fun observeRegions(): Flow<List<String>>
    fun observeSubregions(): Flow<List<String>>
    fun observeSyncStatus(): Flow<SyncStatus>            // Idle(lastSync, total) | Syncing(page, pages) | Failed(error, lastSync)
    suspend fun sync(force: Boolean = false): Result<Unit> // no-op when fresh and !force; serialised with a Mutex
    suspend fun getQuizPool(): List<Country>
}
interface FavoritesRepository { fun observeFavorites(): Flow<List<CountrySummary>>; fun observeIsFavorite(uuid): Flow<Boolean>; suspend fun toggle(uuid); suspend fun clear() }
interface QuizRepository { suspend fun save(result: QuizResult); fun observeHistory(): Flow<List<QuizResult>>; fun observeBest(mode: QuizMode): Flow<QuizResult?>; suspend fun clear() }
interface SettingsRepository { val settings: Flow<AppSettings>; suspend fun setUnitSystem(..); suspend fun setThemeMode(..); suspend fun setDynamicColor(..) }
```

### 5.5 Sync policy (`CountrySyncManager`)

1. Called from `ContrisApp` on first composition (`SyncCountriesUseCase(force = false)`), from pull-to-refresh, and from Settings → "Refresh now" (`force = true`).
2. Skip if `!force && now - lastSync < 72h && count() > 0`.
3. Fetch pages sequentially with `offset = 0, 100, 200, …` until `meta.more == false`; emit `SyncStatus.Syncing(page, totalPages)`.
4. Map DTO → entity (skip any object without `uuid` or `names.common`), then `replaceAll` in one transaction — the UI never sees a half-synced DB.
5. On success store `last_sync_epoch_ms`, `last_sync_total`. On failure keep existing data and emit `SyncStatus.Failed(error, lastSync)`.
6. Manual refresh is rejected (with a message) if the previous attempt was < 60 s ago.
7. A `WorkManager` periodic job is **out of scope**; refresh happens on app open when stale.

---

## 6. Domain models (essentials)

```kotlin
data class CountrySummary(val uuid: String, val name: String, val capital: String?, val region: String?,
                          val flagEmoji: String?, val flagPng: String?, val population: Long?, val areaKm2: Double?)

data class Country(uuid, names: Names, codes: Codes, capitals: List<Capital>, flag: FlagInfo, region, subregion,
                   continents, landlocked, borders: List<String>, area: Area?, coordinates: LatLng?, population,
                   currencies: List<Currency>, languages: List<Language>, callingCodes, tlds, timezones,
                   drivingSide, carSigns, postalFormat, startOfWeek, gini: Map<Int, Double>, governmentType,
                   classification: Classification, memberships: Memberships, descriptions: Descriptions?, links: Links,
                   lastUpdated: Instant?)

data class CountryFilters(val regions: Set<String> = emptySet(), val subregions: Set<String> = emptySet(),
                          val continents: Set<String> = emptySet(), val landlocked: Boolean? = null,
                          val unMember: Boolean? = null, val sovereignOnly: Boolean = false,
                          val memberships: Set<Membership> = emptySet(), val favoritesOnly: Boolean = false) {
    val activeCount: Int get() = ...
}
enum class CountrySort { NAME_ASC, NAME_DESC, POPULATION_DESC, POPULATION_ASC, AREA_DESC, AREA_ASC }
enum class Membership { UN, EU, EUROZONE, SCHENGEN, NATO, COMMONWEALTH, OECD, G7, G20, BRICS, OPEC, AFRICAN_UNION, ASEAN, ARAB_LEAGUE }
enum class UnitSystem { METRIC, IMPERIAL }; enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class QuizMode { FLAG_TO_COUNTRY, COUNTRY_TO_CAPITAL, CAPITAL_TO_COUNTRY }
data class QuizQuestion(val prompt: QuizPrompt, val options: List<String>, val correctIndex: Int, val countryUuid: String)
sealed interface QuizPrompt { data class Flag(val pngUrl: String, val emoji: String?) ; data class Text(val text: String) }
data class QuizRound(val mode: QuizMode, val questions: List<QuizQuestion>)   // size 10
data class QuizResult(val mode: QuizMode, val score: Int, val total: Int, val bestStreak: Int, val playedAt: Instant)
```

---

## 7. Navigation specification (Navigation 3)

### 7.1 Keys

```kotlin
sealed interface NavKey                                 // androidx.navigation3.runtime.NavKey, all @Serializable
@Serializable data object CountriesKey : NavKey
@Serializable data object FavoritesKey : NavKey
@Serializable data object QuizKey : NavKey
@Serializable data class  CompareKey(val firstUuid: String? = null) : NavKey
@Serializable data object SettingsKey : NavKey
@Serializable data class  CountryDetailKey(val uuid: String) : NavKey
@Serializable data class  QuizPlayKey(val mode: QuizMode) : NavKey
@Serializable data class  QuizResultKey(val mode: QuizMode, val score: Int, val total: Int, val bestStreak: Int) : NavKey
```

### 7.2 Back stacks

- `TopLevelBackStack` (the official Nav3 "common navigation UI" recipe): one `NavBackStack` per tab, persisted with
  `rememberSaveable`; exposes a combined `backStack` for `NavDisplay`, `selectTab(key)`, `add(key)`, `removeLast()`.
- Selecting a tab switches to that tab's stack (history preserved); re-selecting the current tab pops to its root.
- System back pops the current tab's stack; on a root other than Countries it returns to Countries; on Countries it exits.
- `NavDisplay(entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()))`
  so each entry gets its own ViewModel scope; `hiltViewModel()` from `androidx.hilt:hilt-navigation-compose` resolves inside entries.
- Detail is reachable from Countries, Favorites, Compare and Quiz result; `CountryDetailKey` is pushed onto whichever tab stack is active.
- Argument-bearing ViewModels (`CountryDetailViewModel(uuid)`, `QuizViewModel(mode)`) use `@HiltViewModel(assistedFactory = …)` + `@AssistedInject`, following the Nav3 "passing arguments to ViewModels" recipe.

### 7.3 Shell

`ContrisApp` keeps the existing `NavigationSuiteScaffold` (bottom bar on phones, rail on tablets) with the 5 tabs,
each with a Material icon (`Public`, `Favorite`, `Quiz`, `CompareArrows`, `Settings`). The current `Home/Favorites/Profile`
enum and `Greeting` composable are removed.

---

## 8. Feature specifications

Each feature lists: UiState, UiEvents, UiEffects, ViewModel behaviour, UI, acceptance criteria.

### 8.1 Countries (tab root)

**UiState**
```kotlin
data class CountriesUiState(
    val query: String = "",
    val filters: CountryFilters = CountryFilters(),
    val sort: CountrySort = CountrySort.NAME_ASC,
    val countries: List<CountrySummary> = emptyList(),
    val isLoadingList: Boolean = true,
    val sync: SyncStatus = SyncStatus.Idle(null, 0),
    val isRefreshing: Boolean = false,
    val availableRegions: List<String>, val availableSubregions: List<String>,
    val isFilterSheetOpen: Boolean = false,
    val unitSystem: UnitSystem,
)
```
**UiEvents** `QueryChanged(q)`, `ClearQuery`, `SortChanged(sort)`, `FiltersChanged(filters)`, `ClearFilters`, `OpenFilters`, `CloseFilters`, `Refresh`, `CountryClicked(uuid)`, `RetrySync`.
**UiEffects** `NavigateToDetail(uuid)`, `ShowMessage(UiText)`.

**ViewModel** — `combine(query.debounce(250), filters, sort)` → `ObserveCountriesUseCase` → list; `observeSyncStatus()` merged in. Query/filters/sort are kept in `SavedStateHandle` so they survive process death. `Refresh` calls `sync(force = true)` and maps `SyncError` to messages.

**UI** — `TopAppBar` with title + sort menu; `SearchBar` (docked) below; filter button with active-count badge; `LazyColumn` of `CountryListItem` (flag thumbnail 56×40 via Coil `url_png`, emoji fallback, common name, capital · region, population formatted compactly e.g. "41.8M"). `PullToRefreshBox`. A thin status banner when `sync is Failed` ("Showing data from 3 days ago · Retry") and a progress banner during the first sync. Empty states: "No countries match", first-launch "Downloading countries…". `FilterSheet` is a `ModalBottomSheet` with: region chips (multi), subregion chips (filtered by selected regions), continent chips, toggles (Landlocked / Coastal / Any; UN member; Sovereign only), membership chips; "Clear all" + "Apply".

**Acceptance**
- First launch with network: list populated after 3 requests, no visible flicker between pages (single transaction).
- First launch without network: clear error with Retry; retry succeeds once online.
- Typing "ger" shows Germany, Algeria, Niger…; typing "DE" matches alpha-2; typing "Deutschland" matches native name.
- Filters AND together; count badge reflects active filters; sort order is correct for population/area with nulls last.
- Rotation and process death keep query/filters/sort/scroll position.

### 8.2 Country detail

**UiState** `sealed interface CountryDetailUiState { Loading; NotFound; Content(country: Country, borders: List<CountrySummary>, isFavorite: Boolean, unitSystem: UnitSystem, flagTint: Color?) }`
**UiEvents** `ToggleFavorite`, `BorderClicked(uuid)`, `OpenLink(url)`, `Compare`, `Back`, `ShareClicked`.
**UiEffects** `NavigateToDetail(uuid)`, `NavigateToCompare(firstUuid)`, `OpenUrl(url)`, `Share(text)`, `ShowMessage`.

**ViewModel** — `combine(observeCountry(uuid), observeIsFavorite(uuid), settings)`; borders resolved via `getByAlpha3(country.borders)` once the country arrives. `flagTint` derived from `flag.colors.vibrant ?: prominent` (parsed, alpha-blended with surface so it works in dark mode).

**UI** — `LargeTopAppBar` collapsing header with the SVG flag (Coil SVG decoder, PNG fallback) on a tinted background, title = common name, subtitle = official name; actions: favorite toggle, share, overflow (Wikipedia, Official site, Google Maps, OpenStreetMap). Body sections as cards:
1. **Overview** — short description, capital(s) with role chips, region / subregion / continents, population, area (km² or mi² per unit setting), density (computed), government type, classification badges (Sovereign / UN member / Dependency of X / Disputed).
2. **Flag** — emoji, description text, colour swatches.
3. **Geography** — coordinates, landlocked, timezones, borders as flag+name chips (tappable).
4. **People & culture** — languages (name + native name), demonym not used (not in v5 free fields list), start of week, driving side, measurement system.
5. **Economy** — currencies (code, name, symbol), latest Gini with year; small inline bar for Gini history if ≥ 2 years.
6. **Memberships** — chips for each `true` membership.
7. **Codes & misc** — alpha-2/3, numeric, FIFA, IOC, calling codes, TLDs, postal format.
8. Footer — "Data updated {date}" from `last_updated_epoch`.

**Acceptance** — all sections render with graceful omission when data is missing (no empty cards); border chips navigate; favorite toggle updates instantly (optimistic via DB flow); links open in the browser via `UiEffect`; works for an entry with empty codes (Abkhazia) without crashing.

### 8.3 Favorites (tab root)

**UiState** `FavoritesUiState(favorites: List<CountrySummary>, isLoading: Boolean, sort: CountrySort)`
**UiEvents** `CountryClicked(uuid)`, `Remove(uuid)`, `UndoRemove`, `SortChanged`. **UiEffects** `NavigateToDetail`, `ShowUndoSnackbar`.
**UI** — same `CountryListItem` with swipe-to-remove + Snackbar undo; empty state with a CTA that switches to the Countries tab. Ordered by `added_at` desc by default.
**Acceptance** — adding from detail appears immediately; swipe + undo restores; survives restart.

### 8.4 Compare (tab root)

**UiState**
```kotlin
data class CompareUiState(val first: Country? = null, val second: Country? = null,
                          val rows: List<ComparisonRow> = emptyList(), val pickerFor: Slot? = null,
                          val pickerQuery: String = "", val pickerResults: List<CountrySummary> = emptyList(),
                          val unitSystem: UnitSystem)
data class ComparisonRow(val label: UiText, val left: String?, val right: String?, val winner: Side?)  // winner only for numeric rows
```
**UiEvents** `OpenPicker(slot)`, `PickerQueryChanged`, `CountryPicked(slot, uuid)`, `ClosePicker`, `Swap`, `Clear(slot)`, `OpenDetail(uuid)`.
**ViewModel** — selections stored in `SavedStateHandle` (`firstUuid`, `secondUuid`); `CompareKey.firstUuid` pre-fills slot A when arriving from a detail screen. `BuildComparisonUseCase` produces rows: Population, Area, Density, Capital, Region/Subregion, Languages, Currencies, Driving side, Gini (latest), Landlocked, UN member, Memberships (shared vs. exclusive), Timezones count, Calling code. Numeric rows mark the larger value.
**UI** — two header cards (flag + name, tap to change, × to clear) with a swap button; `LazyColumn` of rows in a 3-column layout (label | left | right), winner cell highlighted with `primaryContainer`; picker is a `ModalBottomSheet` with a search field reusing `ObserveCountriesUseCase`. Empty state explains "Pick two countries".
**Acceptance** — arriving via "Compare" on a detail pre-fills slot A; swap works; selections survive rotation; all rows degrade to "—" when a value is missing.

### 8.5 Quiz (tab root → play → result)

**QuizHome UiState** `QuizHomeUiState(bestByMode: Map<QuizMode, QuizResult?>, recent: List<QuizResult>)`; events `StartQuiz(mode)`, `ClearHistory`.

**QuizPlay UiState**
```kotlin
sealed interface QuizPlayUiState {
    data object Generating : QuizPlayUiState
    data class Question(val index: Int, val total: Int, val question: QuizQuestion, val selectedIndex: Int?,
                        val revealed: Boolean, val score: Int, val streak: Int, val bestStreak: Int) : QuizPlayUiState
    data class Finished(val result: QuizResult) : QuizPlayUiState
}
```
Events `OptionSelected(i)`, `Next`, `Quit`. Effects `NavigateToResult(result)`, `NavigateToDetail(uuid)`.

**Generation (`GenerateQuizRoundUseCase`)** — pool = sovereign countries with a flag PNG and a capital (`getQuizPool`). 10 questions, no repeated correct answer. Distractors: 3 other countries, preferring the same region/subregion (fallback to any) so questions aren't trivial; options shuffled. Mode mapping: `FLAG_TO_COUNTRY` prompt = flag image, options = names; `COUNTRY_TO_CAPITAL` prompt = name (+emoji), options = capitals; `CAPITAL_TO_COUNTRY` prompt = capital, options = names. Deterministic with an injected `Random` for tests.

**UI** — progress indicator "3 / 10"; prompt card (flag image 240 dp wide or large text); 4 option buttons; after selection the correct one turns green and a wrong pick red (uses semantic colours from theme, not raw `Color.Green`), "Next" appears; result screen shows score ring, best streak, "Play again", "See answers" (list of questions with links to country detail), "Back to quiz".

**Acceptance** — a round always has 10 valid questions with 4 distinct options and exactly one correct; score persisted on completion; quitting mid-round does not persist; results survive rotation (state in `SavedStateHandle`-backed ViewModel); best score per mode shown on the Quiz tab.

### 8.6 Settings (tab root)

**UiState** `SettingsUiState(unitSystem, themeMode, dynamicColor, lastSync: Instant?, countryCount: Int, sync: SyncStatus, appVersion: String)`
**UiEvents** `SetUnitSystem`, `SetThemeMode`, `SetDynamicColor`, `RefreshNow`, `ClearFavorites`, `ClearQuizHistory`, `OpenAttribution`.
**UI** — grouped list: *Display* (theme segmented buttons; dynamic colour switch, only on API 31+), *Units* (metric/imperial), *Data* ("Last updated 2 hours ago · 254 countries", "Refresh now" with progress, note about the 3-day cache), *Reset* (clear favourites / quiz history with confirmation dialogs), *About* (version, "Data by REST Countries" link, licences). `ContrisTheme` reads `themeMode`/`dynamicColor` from `SettingsRepository` via `MainActivity`.
**Acceptance** — theme changes apply immediately app-wide; unit change updates detail/compare/list formatting; Refresh respects the 60 s debounce and shows quota/auth errors verbatim-but-friendly.

### 8.7 Shared UI components

`FlagImage(png, svg?, emoji, size)` (Coil `AsyncImage`, crossfade, emoji placeholder, rounded 4 dp, subtle outline for white flags) · `CountryListItem` · `StatRow(label, value)` · `ChipRow` · `SectionCard(title) { }` · `LoadingState` / `EmptyState(icon, title, body, action?)` / `ErrorState(message, onRetry)` · `UiText` (`Dynamic(String) | Resource(@StringRes, args)`) · formatters: `formatPopulation` (compact "41.8M" in lists, full "41,798,407" in detail), `formatArea(unitSystem)`, `formatDensity`, `formatRelativeTime`.

Theme: keep Material 3 dynamic colour; add a brand seed colour scheme for < API 31 and when dynamic colour is off; add `extendedColors` for quiz correct/incorrect; typography unchanged.

---

## 9. Testing specification

| Layer | Tool | What |
|---|---|---|
| Mappers | JUnit + Truth | DTO→entity→domain for the Canada sample fixture; empty-code entry (Abkhazia); missing/null branches; `search_text` and `gini_latest` derivation. |
| Network | Retrofit + MockWebServer | Auth header present; `response_fields_omit` sent; envelope parsing; error mapping for 401/403/429/500; unknown fields ignored. |
| DAO | Room in-memory + Robolectric | `replaceAll` removes stale rows; `CountryQueryBuilder` search/filters/sort combinations; favorites join; quiz pool predicate. |
| Repositories / Sync | coroutines-test + Turbine + MockK | Staleness rule (72 h), forced sync, sequential paging until `more=false`, failure keeps old data, mutex prevents concurrent syncs, 60 s debounce. |
| Use cases | JUnit | `GenerateQuizRoundUseCase` invariants (10 questions, 4 unique options, 1 correct, same-region preference) with a seeded `Random`; `BuildComparisonUseCase` winner logic and "—" fallbacks. |
| ViewModels | coroutines-test + Turbine | Each ViewModel: initial state, event→state transitions, effects emitted once, `SavedStateHandle` restore. Fake repositories (in-memory) rather than mocks where practical. |
| Compose UI | `compose-ui-test-junit4` (androidTest) | Countries: search filters list, filter sheet applies, click navigates (fake nav). Detail: sections and favorite toggle. Quiz: select → reveal → next → finish. Screens are tested through the stateless `XScreen(state, onEvent)` with fake state. |
| Hilt | `hilt-android-testing` | One instrumented smoke test launching `MainActivity` with a test `NetworkModule` replaced by MockWebServer fixtures. |

Fixtures: `app/src/test/resources/fixtures/countries_page_{0,1,2}.json` built from real responses (translations/palette/leaders removed), plus `error_401.json`, `error_429.json`.

CI-style commands:
```bash
./gradlew :app:testDebugUnitTest
```
```bash
./gradlew :app:connectedDebugAndroidTest
```

---

## 10. Implementation phases

Each phase ends with a compiling app and green tests. Checkboxes are the task list for implementation.

### Phase 0 — Foundations
- [ ] Add versions/libraries/plugins to `libs.versions.toml`; bump Compose BOM, core, activity, lifecycle.
- [ ] Apply KSP, Hilt, kotlinx-serialization, Room plugins; `buildConfig = true`; API key + base URL `buildConfigField`s; Java 17.
- [ ] `ContrisApplication` (`@HiltAndroidApp`), manifest `android:name`, `INTERNET` permission.
- [ ] `DispatcherProvider` + `DispatcherModule`.
- [ ] Navigation skeleton: `NavKeys`, `TopLevelBackStack`, `NavDisplay` with 5 placeholder tab screens; remove `Greeting`/`AppDestinations`.
- [ ] Shared components + `UiText` + formatters (with unit tests for formatters).
- [ ] Verify `./gradlew :app:assembleDebug` with `JAVA_HOME` set to the JBR.

### Phase 1 — Data layer & sync
- [ ] DTOs, `RestCountriesApi`, `AuthInterceptor`, `NetworkModule` (OkHttp, Retrofit, Json, Coil `ImageLoader` with OkHttp + SVG).
- [ ] Room entities, converters, DAOs, `ContrisDatabase`, `DatabaseModule`, exported schema.
- [ ] Mappers + `CountryQueryBuilder`.
- [ ] `SettingsDataStore` + `SettingsRepositoryImpl`.
- [ ] `CountryRepositoryImpl` + `CountrySyncManager` (status flow, staleness, paging, mutex, debounce, error mapping).
- [ ] Use cases: `SyncCountriesUseCase`, `ObserveCountriesUseCase`, `ObserveCountryUseCase`.
- [ ] Tests: mappers, MockWebServer, DAO, sync manager.

### Phase 2 — Countries list
- [ ] `CountriesUiState/UiEvent/UiEffect`, `CountriesViewModel` (debounced search, SavedStateHandle).
- [ ] `CountriesScreen` + `FilterSheet` + sort menu + pull-to-refresh + sync banners + empty/error states.
- [ ] Trigger initial sync from app start; navigate to detail.
- [ ] ViewModel tests + Compose UI test.

### Phase 3 — Country detail
- [ ] `CountryDetailViewModel` (assisted `uuid`), borders resolution, flag tint.
- [ ] Detail screen sections, links, share; favorite toggle (depends on Phase 4 repo — implement `FavoritesRepository` here).
- [ ] Tests.

### Phase 4 — Favorites
- [ ] `FavoriteEntity/Dao`, `FavoritesRepositoryImpl`, `ToggleFavoriteUseCase`, `ObserveFavoritesUseCase`.
- [ ] Favorites screen with swipe-to-remove + undo; "favoritesOnly" filter support in `CountryQueryBuilder`.
- [ ] Tests.

### Phase 5 — Compare
- [ ] `BuildComparisonUseCase`, `CompareViewModel`, `CompareScreen`, `CountryPickerSheet`; "Compare" action on detail.
- [ ] Tests (use case + ViewModel).

### Phase 6 — Quiz
- [ ] `QuizResultEntity/Dao`, `QuizRepositoryImpl`, `GenerateQuizRoundUseCase`, `SaveQuizResultUseCase`.
- [ ] Quiz home, play, result screens and ViewModels; nav keys.
- [ ] Tests (generator invariants, ViewModel flow, UI test).

### Phase 7 — Settings & theming
- [ ] `SettingsViewModel/Screen`; theme mode + dynamic colour wired into `ContrisTheme`; units wired into formatters; refresh/clear actions; about/attribution.
- [ ] Tests.

### Phase 8 — Polish & release readiness
- [ ] Adaptive layouts: list–detail two-pane on expanded widths (`material3-adaptive` + Nav3 `ListDetailSceneStrategy`) — stretch goal.
- [ ] Content descriptions / TalkBack pass, large-font check, RTL sanity.
- [ ] R8 release build (`assembleRelease`) verification: kotlinx-serialization and Retrofit keep rules (`app/src/main/keepRules/rules.keep`), run the app from the release APK.
- [ ] Hilt instrumented smoke test; final test run; update `README.md` with setup (API key line in `local.properties`, JAVA_HOME).

---

## 11. Risks & mitigations

| Risk | Mitigation |
|---|---|
| API schema drift (v5 is stable but fields are "reviewed weekly") | Lenient JSON config, nullable DTOs, mapper tests on real fixtures, `uuid` as key. |
| Free quota (1,000/mo) during development | Sync only when stale/forced; MockWebServer fixtures for tests; do **not** point unit tests at the live API. |
| Hilt + AGP 9 built-in Kotlin | Hilt 2.59+ supports AGP 9; KSP only (no kapt). Verified versions in §4. |
| Navigation 3 API still evolving | Pin 1.2.0; isolate all Nav3 usage in `ui/navigation` so changes are local. |
| Large first-launch payload (~3 × ~700 KB with omissions) | Progress banner; single transaction insert; Coil disk cache for flags. |
| Flag PNG `w640` may be heavier than needed for 56 dp thumbnails | Coil downsampling + memory/disk cache; revisit if a smaller rendition URL is confirmed. |
| Non-ISO entries (empty codes, no borders) | All code fields nullable; UI hides empty rows; quiz pool filters `sovereign = 1`. |

---

## 12. Out of scope (for now)

Background periodic sync (WorkManager) · Leaders data (paid tier) · Translations / multi-language content · Map rendering inside the app (links out instead) · Widgets · Multi-module split.

---

## Sources

- REST Countries docs: https://restcountries.com/docs and https://restcountries.com/docs/countries (verified with live demo and real requests on 2026-10-07).
- Hilt Gradle setup: https://dagger.dev/hilt/gradle-setup · AGP 9 support landed in Dagger 2.59 (release notes).
- AGP 9 built-in Kotlin: https://developer.android.com/build/releases/agp-9-0-0-release-notes · https://developer.android.com/r/tools/built-in-kotlin
- Navigation 3 ViewModel scoping & Hilt argument recipe: https://developer.android.com/guide/navigation/navigation-3/save-state · https://developer.android.com/guide/navigation/navigation-3/recipes/passingarguments
- Library versions: Google Maven (`dl.google.com/dl/android/maven2`) and Maven Central metadata, 2026-10-07.
