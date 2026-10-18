# Qualité de l'eau

An Android app that shows the quality of drinking water in every French municipality,
with the drought restrictions, natural and industrial risks, and live river data around it.
All data comes from official French open-data services and needs no account or API key.

The app is in French, with an English translation that follows the phone's language or
the switch in the Sources tab.

## Features

- **Map** of France with the compliance of the latest sample in large cities, plus layers
  for live rivers, groundwater points and river flow stations.
- **Search** by municipality name or postcode, or from the phone's location.
- **Municipality page**: the latest sample's status (compliant, with reservations,
  non-compliant), the four key parameters against their limits, the distribution network,
  and cards for drought restrictions and risks.
- **Analysed parameters**: every result of the latest sample, filtered by family.
- **History**: compliance month by month over the last year and the trend of nitrates,
  conductivity, turbidity, pH and E. coli.
- **Drought**: the VigiEau restriction level for each water resource, the prefectoral
  order, and what is restricted for individuals, local authorities, farms and businesses.
- **Risks**: seismic zone, radon category, clay shrink-swell exposure, the Géorisques
  risk families, recognised natural disasters, risk prevention plans, flood areas,
  classified industrial sites with their Seveso level, and polluted soils (soil
  information sectors, sites handled by the authorities, former industrial activities).
- **Nearby**: the closest live river height and flow, water temperature, flow
  observation stations with their latest flow class (flowing, low, dry…), and the
  nearest groundwater monitoring point with its last sampling: nitrates, total
  pesticides and how many substances were quantified.
- **Compare** two municipalities parameter by parameter.
- **Follow** municipalities and get a notification when a new sample is published, when
  the water is non-compliant or when a quality reference is exceeded.
- **CSV export** of the latest sample's results.

Statuses are never told apart by colour alone: compliant is a round mark with a tick,
reservations a square with "!", non-compliant a diamond with a cross.

## Data sources

| Source | Used for |
|---|---|
| [Hub'Eau, qualité de l'eau potable](https://hubeau.eaufrance.fr/page/api-qualite-eau-potable) | Samples and analysis results (health monitoring by the Ministry of Health) |
| [Hub'Eau hydrométrie, température, écoulement, qualité des nappes](https://hubeau.eaufrance.fr) | Live rivers, water temperature, flow stations, groundwater points |
| [VigiEau](https://vigieau.gouv.fr) | Drought restriction zones and orders |
| [Géorisques](https://www.georisques.gouv.fr) | Risks, natural disasters, radon, seismic zones, classified sites |
| [API Découpage administratif](https://geo.api.gouv.fr) | Municipality search, postcodes, coordinates |

Drinking-water results are published monthly and are often one to three months old; the
app always shows the sampling date. Answers are cached on the device (10 minutes for live
river readings, 3 hours for drought zones, 24 hours otherwise) and served from the cache
when offline.

## Building

Requirements: JDK 17 or newer (CI uses 25) and the Android SDK with API 37.

```bash
./gradlew assembleDebug          # debug APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # unit and Robolectric tests
./gradlew lintDebug              # lint, NewApi is fatal
./gradlew bundleRelease lintVitalRelease
./gradlew connectedDebugAndroidTest   # instrumented tests, needs a device or emulator
```

A signed release build reads `RELEASE_KEYSTORE_FILE`, `RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` from Gradle properties or the environment.

`ScreensQaTest` renders every screen against recorded API answers and writes screenshots
to `app/build/qa-screenshots/`; CI uploads them as the `qa-screenshots` artifact.
`ScreensDeviceTest` renders the same screens on an Android emulator, which CI boots for
every push and pull request.

## Architecture

Kotlin, Jetpack Compose and Material 3, a single `:app` module, min SDK 24, target SDK 36.

```
app/src/main/java/fr/mandarine/environmentquality/
├── domain/       Pure Kotlin models, repository interfaces and use cases
├── data/         Hub'Eau, geo.api.gouv.fr, VigiEau and Géorisques clients, Room cache,
│                 location, alert worker and notifications
├── presentation/ ViewModels and screen states
└── ui/           Compose screens, components, theme and Navigation 3
```

Dependencies are wired by hand in `AppContainer`. Background alerts run as a WorkManager
job twice a day. The map is drawn on a Compose canvas from a France outline, without map
tiles. See `specs.md` for the product definition and `CLAUDE.md` for contributor
conventions.
