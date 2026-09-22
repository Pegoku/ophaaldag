# Security policy

## Supported versions

Only the latest release is supported. Fixes ship in a new release rather than as patches to
older versions.

## Reporting a vulnerability

Please report security issues privately through
[GitHub's private vulnerability reporting](https://github.com/Pegoku/ophaaldag/security/advisories/new)
rather than in a public issue. Expect a first reply within a week.

## Scope

This repository covers the Android app only. The backend it talks to
(`api.mijnafvalwijzer.nl`) belongs to AddComm and is out of scope here. Report issues in
that service to AddComm or Cure Afvalbeheer directly. The API key in `CureApi.kt` is the
static app identifier shipped in the official app; it is not a user secret and is not
treated as one.

The app stores an address and settings in local DataStore, has no account, no analytics and
no push registration. The signing key is never in this repository; CI reads it from
repository secrets.
