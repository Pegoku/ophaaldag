# Ophaaldag

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
- Dynamic colour (Material You) with a green fallback palette, Dutch and English UI

## App icon

The [SVG logo](artwork/ophaaldag.svg) uses `currentColor` for easy recoloring when
embedded inline on the web. Its matching Android vector supplies both the normal
white-on-green launcher icon and the monochrome adaptive icon. On Android 13+
with a supported launcher, enable **Themed icons** in the launcher's wallpaper/style
settings to use system wallpaper colors automatically.

## Build

Requirements: JDK 21, Android SDK with platform 37 (Android Studio installs it).

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

```
app/src/main/kotlin/com/pegoku/ophaaldag/
  data/        API client, JSON parser, models, settings (DataStore), repository + disk cache
  reminders/   AlarmManager-based reminder scheduling and notification
  widget/      Glance home-screen widget
  ui/          Compose UI: theme, root navigation, screens
docs/API.md    Reverse-engineered API documentation
re/samples/    API response fixture with synthetic address data
```

## How the API was obtained

The official APK was pulled from a phone with `adb pull`, decompiled with jadx, and the
network calls in `HttpSpul`, `PhonePostcodeSelect`, `MyFirebaseServerUtilities` and friends
were traced and then verified live. Decompiled sources and the APK are not part of this repo.

## Privacy

Read the [privacy policy](https://pegoku.github.io/ophaaldag/privacy.html).

## Calendar updates

Enable calendar sync in Settings and choose your Google calendar to receive new dates when
the waste service publishes them. Background refresh is scheduled roughly every six hours;
Android may delay it. Exported `.ics` files are one-time snapshots and never update automatically.
The home screen shows today and future pickups; past pickups remain visible in the month calendar.

## License

Copyright (C) 2026 Pere Gomila.

Ophaaldag is free software, licensed under the
[GNU General Public License v3.0 or later](LICENSE).

This is a copyleft licence. In short, if you distribute this app or anything derived from
it - a fork, a rebranded build, an app that reuses parts of this code - you must:

- release your version's **complete source code** under the GPL v3 (or later) as well,
- **keep the copyright notice and credit the original author, Pere Gomila**, and link back
  to <https://github.com/Pegoku/ophaaldag>,
- state what you changed, and pass on these same freedoms to your users.

You may not relicense this code under a proprietary or closed-source licence. See the
[LICENSE](LICENSE) file for the full terms, which are what legally apply.

Pull requests are welcome and are accepted under the same licence.
