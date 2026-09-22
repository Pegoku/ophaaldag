# Contributing

Thanks for taking a look. Issues and pull requests are welcome.

## Before you start

- For anything larger than a bug fix or a translation tweak, open an issue first so we can
  agree on the approach before you spend time on it.
- Contributions are accepted under the project's licence,
  [GPL-3.0-or-later](LICENSE). By opening a pull request you agree to that.

## Building

Requirements: JDK 21 and the Android SDK with platform 37 (Android Studio installs it).

```sh
./gradlew :app:assembleDebug
./gradlew :app:lintRelease :app:testDebugUnitTest
adb install app/build/outputs/apk/debug/app-debug.apk
```

The debug build uses the application ID `com.pegoku.ophaaldag.debug`, so it installs
alongside a release build instead of replacing it. Release signing is optional: without a
`keystore.properties` or the matching environment variables, the release build is simply
left unsigned.

## Pull requests

- Keep commits in coherent units. Subjects are short, lowercase and imperative
  (`fix address race`, `add container filter chips`) with no trailing period.
- `./gradlew :app:lintRelease :app:testDebugUnitTest` must pass; CI runs the same commands.
- Add or update unit tests when you change parsing, date handling, clustering or export
  logic — those all have test coverage in `app/src/test/`.
- UI changes: please attach a screenshot, in both light and dark theme when relevant.
- User-visible strings go in `app/src/main/res/values/strings.xml` and
  `values-nl/strings.xml`. Never hardcode a string in a composable.

## Translations

The app ships English and Dutch. To add a language, copy `app/src/main/res/values/strings.xml`
to `values-<code>/strings.xml`, translate the values, and add the code to
`resourceConfigurations` in `app/build.gradle.kts`.

## The upstream API

The backend is not ours — it is AddComm's MijnAfvalwijzer platform, documented in
[docs/API.md](docs/API.md) from reverse engineering the official app. Please be considerate
with it: no scraping loops, no load testing, and keep any live checks to a handful of
requests. The app caches aggressively for the same reason.

## Reporting a bug

Include your Android version, the device, the app version (Settings → About) and, when the
problem is address-specific, the municipality — not your full address.
