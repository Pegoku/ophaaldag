# Cure M3

A community-made, Material 3 Expressive Android client for the **Cure Afvalbeheer** waste
calendar (Eindhoven, Geldrop-Mierlo, Valkenswaard). It talks to the same public web service
as the official `nl.opzet.cure` app, documented in [docs/API.md](docs/API.md).

Not affiliated with Cure Afvalbeheer or AddComm.

## Features

- Pickup calendar: next pickup hero, upcoming list, month grid with per-stream colours and filters
- Waste guide: separation info per stream plus a searchable "Afval-ABC" of ~1,900 household items
- Announcements, service-message history and municipality tips/contact pages
- Nearby public containers (glass, paper, textile, residual) sorted by distance, opens in Maps
- Local reminders (evening before or morning of) per waste stream, no push registration needed
- Home-screen widget with the next pickups
- Offline: the last document is cached and shown instantly; pull to refresh
- Dynamic colour (Material You) with a Cure-green fallback palette, Dutch and English UI

## Build

Requirements: JDK 17+, Android SDK with platform 37 (Android Studio installs it).

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

```
app/src/main/kotlin/com/pegoku/curem3/
  data/        API client, JSON parser, models, settings (DataStore), repository + disk cache
  reminders/   AlarmManager-based reminder scheduling and notification
  widget/      Glance home-screen widget
  ui/          Compose UI: theme, root navigation, screens
docs/API.md    Reverse-engineered API documentation
re/samples/    Trimmed real API response used as a unit-test fixture
```

## How the API was obtained

The official APK was pulled from a phone with `adb pull`, decompiled with jadx, and the
network calls in `HttpSpul`, `PhonePostcodeSelect`, `MyFirebaseServerUtilities` and friends
were traced and then verified live. Decompiled sources and the APK are not part of this repo.

