# Remote Config (Firebase) for ORBIT

ORBIT reads a small set of remote values (which ads are on, capping, the first-run flow, premium close behaviour).
**Today no Firebase project exists for ORBIT**, so the app runs on the in-code defaults. Everything below is what the
owner needs to do once to switch the remote layer on. Nothing else in the app changes.

## How it works

* `core/remote/AdConfig.kt` - immutable model + `AdConfig.parse(lookup)`: absent / blank / malformed values fall back to
  the defaults, numbers are clamped. `AdConfig.DEFAULT` equals `res/xml/remote_config_defaults.xml` (keep both in sync).
* `core/remote/RemoteConfigRepository.kt`
  * `FirebaseRemoteConfigRepository` (production) - initialises Firebase **manually** with `FirebaseOptions` taken from
    `core/remote/RemoteConfigIds.kt` (no `google-services.json`, no google-services plugin). If the three constants are blank Firebase
    is **never touched** and `config` stays at `AdConfig.DEFAULT` (this is the current state).
  * `fetch(timeoutMs)` - (1) applies the last activated values (works offline, instantly), (2) when online fetches and
    activates new values within the timeout (the startup passes `splash_max_wait_seconds`, default 7s). Minimum fetch
    interval: 3s in debug builds, 12h in release builds. It never throws.
  * Offline or fetch failure -> the previous values (or defaults) stay in force.
* Only `firebase-config` (+ BoM) is added. **No Analytics, Crashlytics or FCM.**
* Ad unit ids are NOT remote values: they live in `core/ads/AdIds.kt` (sample ids in debug, release constants pasted by the owner). No id lives in Gradle.

## 1. Create the Firebase project (one time)

1. https://console.firebase.google.com -> Add project (disable Google Analytics when asked).
2. Project settings -> General -> Your apps -> Add app -> Android, package name **`com.funsol.orbit.planner`**.
   You do not need to download `google-services.json`.
3. Copy these three values from the app's settings page:
   * Web API Key (Project settings -> General) -> `RemoteConfigIds.API_KEY`
   * App ID (looks like `1:1234567890:android:abcdef...`) -> `RemoteConfigIds.APP_ID`
   * Project ID -> `RemoteConfigIds.PROJECT_ID`
4. Paste them between the quotes in `app/src/main/java/com/funsol/orbit/core/remote/RemoteConfigIds.kt`:

   ```kotlin
   const val API_KEY = "AIza..."
   const val APP_ID = "1:1234567890:android:abcdef..."
   const val PROJECT_ID = "my-project-id"
   ```

   Blank constants = Firebase is not initialised (in-code defaults). No Gradle property is involved.
5. In the API key's Google Cloud restrictions allow the **Firebase Remote Config API** and
   **Firebase Installations API**, and restrict the key to the Android package + release SHA-1.

## 2. Publish the parameters (Firebase console -> Remote Config)

Publish only the keys you want to change; every other key keeps its default. All values are strings in the console.

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `ads_enabled` | bool | `true` | Master switch for every ad. |
| `banner_enabled` | bool | `true` | Inner banner on/off. |
| `banner_screens` | csv | `home,tasks,calendar,more` | Tabs that may show the banner. |
| `native_language_enabled` | bool | `true` | Master switch of the Language-screen ad (rect banner or native). |
| `language_ad_type` | `rect` / `native` | `rect` | Language screen ad: `rect` = 300x250 medium-rectangle banner in a fixed bottom slot (default), `native` = the large native card. Anything else = `rect`. |
| `native_onboarding_enabled` | bool | `true` | Native ads on the onboarding pages. |
| `native_exit_enabled` | bool | `true` | Native ad in the Exit sheet. |
| `native_home_enabled` | bool | `true` | Native ad between Home sections. |
| `interstitial_splash_first_run` | bool | `false` | Splash interstitial on the very first launch. |
| `interstitial_splash_returning` | bool | `true` | Splash interstitial on later launches. |
| `interstitial_nav_enabled` | bool | `true` | Interstitials at natural transitions. |
| `interstitial_nav_every_n_actions` | int 1..50 | `4` | Natural transitions between two interstitials. |
| `interstitial_min_interval_seconds` | int 0..3600 | `90` | Minimum seconds between two full-screen ads. |
| `app_open_enabled` | bool | `false` | App-open ad when returning to the app. |
| `app_open_min_background_seconds` | int | `60` | Minimum time in background before an app-open ad. |
| `splash_max_wait_seconds` | int 2..15 | `7` | Remote-config fetch timeout; the splash hard cap is this + 3s. |
| `onboarding_flow_first_run` | JSON | `""` | First-run flow (see below). Blank = built-in. |
| `onboarding_flow_returning` | JSON | `""` | Flow for later launches. Blank = built-in. |
| `premium_close_mode` | int 0..3 | `2` | 0 hard paywall, 1 close at end now, 2 close at end after 3s, 3 close at start after 3s. |
| `premium_variant_inner` | int 1..3 | `1` | Premium art page for in-app entries. |

### Flow JSON

```json
{"screens":[
  {"id":"SPLASH_INTERSTITIAL","enabled":true},
  {"id":"LANGUAGE"},
  {"id":"ONBOARDING"},
  {"id":"PREMIUM","enabled":true,"screenType":2},
  {"id":"HOME"}
]}
```

* `id` (case-insensitive): `SPLASH_INTERSTITIAL`, `LANGUAGE`, `ONBOARDING`, `PREMIUM`, `HOME`. Unknown ids are ignored.
* `enabled` defaults to `true`; `screenType` (1..3) is the premium art page.
* The **order in the JSON is ignored** - ORBIT always walks splash-ad, language, onboarding, premium, home - the JSON
  switches screens on/off and chooses the premium variant. Steps the user already completed are skipped, and PREMIUM is
  skipped for premium users.
* Invalid JSON, a non-object, or no valid screen -> the built-in flow. Parsing is unit tested (`FlowConfigParserTest`).

## 3. Verify

* Debug build: values refresh within ~3 s of publishing (minimum fetch interval 3s). Release: up to 12 h.
* With the properties blank the app must still start and behave exactly like the defaults above (offline-safe).
