# Cure Afvalbeheer app - backend API

Reverse-engineered from the official Android app `nl.opzet.cure` (v5.40, versionCode 500020),
pulled from a Pixel 8 Pro via `adb pull` and decompiled with jadx. The app is a white-label
build of **AddComm's "MijnAfvalwijzer"** platform; every white-label (Cure, Afvalwijzer, …) talks
to the same backend and only differs by `app_name`, theming and the Firebase project.

Everything below was verified live against the API on 2026-09-19.

## Base URL

```
https://api.mijnafvalwijzer.nl/webservices/appsinput/?apikey=<APIKEY>&method=<method>&…
```

| Item | Value |
|---|---|
| Host | `api.mijnafvalwijzer.nl` (Cloudflare fronted, HTTP/2) |
| Path | `/webservices/appsinput/` |
| API key (string resource `globalnew`) | `5ef443e778f41c4f75c69459eea6e6ae0c2d92de729aa0fc61653815fbd6a8ca` |
| Health check (`globaltest`) | `GET https://api.mijnafvalwijzer.nl/webservices/testConnection` → body `YES` |
| Auth | None beyond the static API key. No cookies needed (a `AFVALWIJZER-API-LB` LB cookie is set, ignorable). |
| Content-Type of responses | `text/html; charset=UTF-8` even though the body is JSON (or plain `OK` / `NOK`). |
| Cache headers | `no-store` |

The API key is baked into the APK, is identical for every user of the app, and is only an
"app identifier". It is not a user secret.

### Common query parameters

Every address-bound method takes the same address tuple:

| Param | Meaning | Notes |
|---|---|---|
| `postcode` | Dutch postcode, no space, upper-case (`1234AB`) | App strips spaces and upper-cases. |
| `huisnummer` | House number (`1`) | |
| `toevoeging` | House number suffix / letter (`D`, or empty) | Called `letter` in the response. |
| `street` | Street name | Only used to disambiguate a postcode with multiple streets. Empty works when `streetList` returns one street. Spaces are sent as `+`. |
| `app_name` | `cure` | Lower-cased `app_name` string resource. Selects the white-label (theming, texts, `gemeente` restriction). |
| `platform` | `phone` or `tablet` | |
| `mobiletype` | `android` | |
| `langs` | `nl` or `en` | Language of the returned texts. Available languages come from `languagesList`. |
| `version` | App version string (`5.40`) | |

## Response envelope

Almost every JSON answer is:

```json
{ "response": "OK" | "NOK", "data": <payload>, "error": false | "" | "<message>" }
```

`error` is polymorphic: boolean `false` on success in some sections, an empty string in others,
and a string message on failure. Parse it as an untyped JSON element.

Nested sections inside `postcodecheck` reuse the same envelope per section, so a single
"OK" top-level reply can contain "NOK" sub-sections (e.g. a municipality without containers).

---

## Methods

### `streetList` - GET

Streets for a postcode. Used to fill the street picker on the address screen.

```
GET …&method=streetList&postcode=1234AB
→ {"response":"OK","data":["Voorbeeldstraat"],"error":false}
```

Unknown postcode → `{"response":"OK","data":[],"error":false}` (empty list, still "OK").

### `languagesList` - GET

Languages available for the municipality behind a postcode.

```
GET …&method=languagesList&postcode=1234AB
→ {"response":"OK","data":[{"name":"Nederlands","lang":"nl","icon":"data:image/png;base64,…"},
                            {"name":"English","lang":"en","icon":"data:image/png;base64,…"}],
   "error":false}
```

### `tinyPostcodeCheck` - GET

Cheap existence check for an address before doing the heavy `postcodecheck`.
Returns **plain text**, not JSON.

```
GET …&method=tinyPostcodeCheck&postcode=1234AB&street=&huisnummer=1&toevoeging=A&app_name=cure
→ OK        (address known and has waste data)
→ NOK       (no data for this address)
→ <empty>   (postcode not in database)
```

### `postcodecheck` - GET (the main call)

Returns *everything* the app shows for one address in a single ~4 MB JSON document
(most of the weight is the `afvalABC` dictionary, the container list, the translation table
and base64-embedded images).

```
GET …&method=postcodecheck&postcode=1234AB&street=&huisnummer=1&toevoeging=A
     &platform=phone&langs=nl&mobiletype=android&version=5.40&app_name=cure
```

Special demo address used by the app's "demo" button: `postcode=DEMOCURE&huisnummer=0&toevoeging=0`
(resolves to Klipperstraat 10, 5616 KA Eindhoven).

Failure: `{"response":"NOK","data":[],"error":"No Afvaldata"}`.

Success: `{"response":"OK","error":"data retrieved","data":{…}}` where `data` has these keys:

| Key | Type | Content |
|---|---|---|
| `info` | object | Resolved address + versions. See below. |
| `gemeente` | string | Municipality slug, e.g. `"eindhoven"`. |
| `gemeenteImages` | envelope | `{gemeenteLogoV2: "https://static.mijnafvalwijzer.nl/logos/eindhoven.png", gemeenteLogo, gemeenteBanner}` (`"NOK"` when absent). |
| `ophaaldagen` | envelope → `[{nameType, type, date}]` | **Pickup calendar.** `type` is the stable waste-type id (`restafval`, `gft`, `papier`, `pmd`, `glas`, `textiel`, …), `nameType` is the localised label (`"papier en karton"` / `"paper"`), `date` is `YYYY-MM-DD`. Starts a few days in the past and runs to the end of the current year (30 entries for the test address). |
| `ophaaldagenNext` | envelope | Same shape for next year; `NOK "No data"` until the next calendar is published. |
| `afvalABCWasteTypes` | `[string]` | Waste-type ids that exist for this municipality. |
| `afvalABC` | `{ "<item name>": [wasteTypeId, …] }` | "Waste ABC" dictionary: ~1,900 household items mapped to the waste stream(s) they belong to. |
| `scheidingsinfo` | envelope → `[{iconName, afvalTitle, afvalName, text (HTML), companies, push_notification_message}]` | Per-waste-type separation guide. `iconName` matches `ophaaldagen[].type`. |
| `mededelingen` | envelope → `[{id, position, title, description, text (HTML), start_date, date, expiration_date, topImage (data-URI or "")}]` | Announcements. `expiration_date` `"0000-00-00"` means never. |
| `pushData` | envelope → `[{date "YYYY-MM-DD HH:MM:SS", message}]` | History of push messages sent to this municipality. |
| `nieuws` | envelope → `[{title, text}]` | News (generic AddComm marketing on Cure). |
| `tips` | envelope → `[{title, content (HTML), icon, start_date, expiration_date}]` | Tips ("Milieustraat Eindhoven" opening info). |
| `homeTips` | envelope → `[{title, content}]` | Generic. |
| `uwgemeente` | envelope → `[{buttonTitle, title, text (HTML)}]` | "Your municipality" pages (Contact, About Cure). |
| `meerweten` | envelope → HTML string | "More info" page. |
| `overafval` | envelope → HTML string | FAQ. |
| `postcodeContainers` | envelope → `[{WasteType, address, city, gemeente, postcode, OcNumber, latitude, longitude, stosag_status, custom_content, enevo_*}]` | Public containers (Eindhoven: ~1,400 entries: restafval, glas, papier, textiel). |
| `containers` | envelope | Per-address containers (NOK for Cure). |
| `options` | object | Feature flags and theming, see below. |
| `localNotifications` | envelope → `{isEnabled, ListNotifications, ListReminders, timeOption}` | Server-side default for local reminders (empty for Cure). |
| `langs` | envelope → `{KEY: "translation"}` | ~540 UI strings in the requested language. |
| `afroepData`, `balancestext`, `diftartext`, `custompages`, `google_analytics` | envelope | Features not enabled for Cure (all NOK / empty). |

#### `data.info`

```json
{
  "postcode":"1234AB","huisnummer":"1","letter":"A","straat":"Voorbeeldstraat","street":"",
  "plaats":"Eindhoven","latitude":"51.000000","longitude":"5.000000",
  "version":1,
  "afvaldataVersion":"1789791081","contentVersion":"1786352230","iconsVersion":"1708512901",
  "templatesVersion":"NOK","pushNotificationID":"",
  "gemeenteName":"eindhoven","country":"nl","customerID":"352","afvalDataId":"fixture",
  "afvalshopEmail":"","registrationPickupData":{"enable_registration_pickup":false},
  "debtornumber":"","diftar":{…},"diftar_stats":[],"diftar_visible":false,
  "balances_visible":false,"balances_button":"",
  "extraInfo":{"specific_address_info":"","cluster":"","wastetype_info":"","wastetype_info_nextyear":"","route":"","pickups2":""},
  "calendar_uuid":"","show_ical_button":"0",
  "physicalCal":{"response":"NOK","data":false,"error":"NOT ALLOWED"},
  "containers_notif":false,"stook_visible":false,"stook_info":{…},"stookalert_visible":false,
  "extra_menu_links":[],"extra_menu_links_position":-1
}
```

`afvaldataVersion` / `contentVersion` are unix timestamps; the official app shows them as
"Afvaldata: 19/09/2026 06:22:10 / Content: 10/08/2026". Compare them to decide whether a cached
document is stale.

#### `data.options`

```json
{
  "street": {"response":"OK","data":["Voorbeeldstraat"],"error":false},
  "nextYear": {"response":"NOK","data":false,"error":"No exist next year"},
  "customization": {"response":"OK","data":{
      "colorfont":{"colorText":"017800","colorTextOver":"FFFFFF"},
      "colorBackground":{"colorBackground":"407235"},
      "modDate":"1765984962","customColor":"0055A3","enable_background_pickups":null},"error":""},
  "max_tips":10, "period_timer":15,
  "location": {"response":"OK","data":true,"error":""},
  "notifications": {"response":"OK","data":true,"error":""},
  "notificationsTimer": {"response":"OK","data":["15:00","09:00"],"error":""},
  "reportGarbage": {"response":"NOK","data":false,"error":"NOT ALLOWED"},
  "reportGarbageList": {"response":"OK","data":["Storing ondergrondse container","Overige meldingen","Afval naast container"],"error":false},
  "afvalshop": {"response":"NOK","data":false,"error":"NOT ALLOWED"},
  "excludedList":"", "excludedListPickups":[], "gripLocations":[], "gripTimes":[], "gripRoutes":[]
}
```

Colours are RGB hex without `#`. `notificationsTimer` lists the reminder times offered in the
official UI (day before at 15:00, same day at 09:00).

### `createIconsJson` - POST (JSON body)

Icon sync. Body: `{"infoGemeente":{postcode,huisnummer,toevoeging},"defaultIcons":{"fileInfo":[…]},"customIcons":{"fileInfo":[…]}}`.
Response lists icon files to download. Not needed for a re-implementation; the waste-type ids are
stable and can be mapped to local icons.

### Push-notification registration - POST (form-urlencoded)

The official app has **no client-side scheduling**: reminders are Firebase Cloud Messaging pushes
sent by the server according to settings you upload. A third-party app cannot receive those
pushes (they target the Cure Firebase project), so reminders must be implemented locally.
Documented for completeness:

| Method | Form fields |
|---|---|
| `registerDevice` | `regId` (FCM token), `oldRegId`, `mobiletype=android`, `deviceType`, `is_firebase=1`, `deviceInfo`, `postcode`, `street`, `huisnummer`, `toevoeging`, `security_token` (`<postcode>-<10 random alnum>-<huisnummer>`), `app_name` |
| `unRegisterDevice` | `regId` |
| `updateDevicePush` | `regId`, `postcode`, `street`, `huisnummer`, `toevoeging` |
| `saveNotificationsSettings` | `jsonNotif` = JSON `{deviceid, deviceidOld, deviceData:{app_name, brand_info, device_type, entry, lang}, postcodeData:{postcode, street, huisnummer, toevoeging, gemeente}, settings:[{waste_type, time:"HH:mm"}]}` → plain `OK`/`NOK` |

### User-generated content - POST (form-urlencoded / multipart)

| Method | Fields | Notes |
|---|---|---|
| `sendStuureentip` | `postcode, huisnummer, toevoeging, name, email, tip, langs` | "Send a tip" form. |
| `uploadPicture` | `postcode, street, huisnummer, toevoeging, phoneuser, emailuser, containernr, textMessage, wasteType, langs, filenames, LongLat` + image parts | "Report garbage". Disabled for Cure (`options.reportGarbage` = NOK). |
| `requestpickup` / `cancelrequestpickup` | address tuple + `wastetype, date, email, device_id` (+ `confirmed_at` for cancel) | Bulky-waste pickup booking. Disabled for Cure (`registrationPickupData.enable_registration_pickup=false`). |

---

## Other endpoints / assets

* Municipality logo: `https://static.mijnafvalwijzer.nl/logos/<gemeente>.png`
* Web calendar (same data, HTML): `https://afvalkalender.cure-afvalbeheer.nl/`
* Firebase project used for push: `cure-app-e803e`

## Behaviour of the official app worth knowing

* Stores the full `postcodecheck` JSON in `SharedPreferences` (`httpcontent`) and re-fetches on
  every cold start, blocking the UI with a progress dialog.
* Uses deprecated Apache `DefaultHttpClient`, `cleartextTrafficPermitted=true`, and hard-codes
  `screenOrientation="portrait"`.
* All texts (`scheidingsinfo`, `mededelingen`, `tips`, `uwgemeente`, `meerweten`, `overafval`)
  are HTML fragments rendered in `WebView`s.
* Waste-type ids seen: `restafval`, `gft`, `papier`, `pmd`, `glas`, `textiel`, `grofvuil`,
  `milieustraat`, `elec`, `kca`, `asbest`, `sloopafval`, plus `takken`, `kerstbomen`, `pbd`,
  `drocos`, `luiers` in other municipalities.

Address examples other than DEMOCURE are synthetic and are not valid reviewer credentials.
