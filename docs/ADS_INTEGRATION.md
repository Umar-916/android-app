# Ads, consent and offline logic - integration guide

Stack: Next-Gen Google Mobile Ads SDK `1.5.0` (package `com.google.android.libraries.ads.mobile.sdk`), UMP `4.0.0`,
Firebase Remote Config (BoM `35.0.2`, manual init, see `REMOTE_CONFIG.md`). The legacy `play-services-ads*` artifacts are
excluded in every configuration. User data (tasks, notes, ...) never leaves the device; the network is used only for ads,
the consent form and remote config.

## Ad ids (NO ids in Gradle)

* **Debug builds use Google's public sample ids only**: test ads, never real ones.
* **AdMob APP id** = string resource `admob_app_id` (manifest `com.google.android.gms.ads.APPLICATION_ID`):
  * `app/src/debug/res/values/ad_ids.xml` - Google's sample app id.
  * `app/src/release/res/values/ad_ids.xml` - placeholder `REPLACE_WITH_REAL_ADMOB_APP_ID`: **paste the real app id here**.
* **Ad UNIT ids** = `core/ads/AdIds.kt` (a Kotlin helper, like a constants class): debug uses the `SAMPLE_*` constants;
  release uses `RELEASE_BANNER`, `RELEASE_RECT_BANNER` (Language rectangle; falls back to `RELEASE_BANNER` when blank),
  `RELEASE_NATIVE`, `RELEASE_INTERSTITIAL`, `RELEASE_APP_OPEN`. They are blank until the owner pastes the real ids; a
  blank id disables that placement at runtime (`AdDecision.NO_AD_UNIT`, no request, and a sample id is never used in
  release - `AdIds.forBuild` also blanks a sample id found in a release constant).
* **Release guard** (`verifyReleaseAdIds` in `app/build.gradle.kts`, wired to `assembleRelease`/`bundleRelease`/
  `packageRelease`): READS the two files above (no ids stored in Gradle) and FAILS the build when the release
  `admob_app_id` is still the placeholder / a Google sample id / not of the form `ca-app-pub-<16 digits>~<10 digits>`, or
  when a `RELEASE_*` constant in `AdIds.kt` is a Google sample id. Blank unit ids only print a warning.
* Firebase options live in `core/remote/RemoteConfigIds.kt` (blank = Firebase not initialised), see `REMOTE_CONFIG.md`.
* Manifest: `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`, `OPTIMIZE_INITIALIZATION`, `OPTIMIZE_AD_LOADING`,
  `usesCleartextTraffic="false"`.

## Ad design (re-implemented after the reference app, in ORBIT colours)

* Native cards are flat (14dp radius, app card colours, hairline border, 12dp padding). **Compact** = 40dp icon, bold
  headline + "AD" pill, one-line body, full-width CTA: 114dp total. **Large** (Exit sheet, Language when
  `language_ad_type=native`, Home when used) = same header, 110dp media area (aspect kept inside it), body, CTA: ~252dp
  total. CTA = 40dp high, radius 10dp, bold white text on solid teal `#0B8FAD` (visibly unlike the violet gradient
  buttons). Hosts are capped with `heightIn(max)`; skeletons use the same heights.
* Onboarding bottom: with an ad -> row (dots at the start, small "Next" pill at the end) + 12dp + compact ad; without an
  ad -> centered dots above a full-width button (animated switch). State holder: `rememberNativeAdState(placement)`
  (`NativeAdSlotState.available`) / `rememberNativeAdAvailable(placement)`.
* Inner banner: reserved adaptive height above the bottom bar (placeholder "Loading ad" while loading), hairline top
  divider, 4dp gap to the bar, hidden with the keyboard, collapses on no fill.
* Language screen: `RectBannerAdSlot` (fixed 300x250 container in the Scaffold `bottomBar`, skipped on screens shorter
  than 640dp) or the large native, selected by Remote Config `language_ad_type` (`rect` default | `native`).

## Architecture (package `com.funsol.orbit.core.*`)

| Piece | File | Role |
| --- | --- | --- |
| `NetworkObserver` / `ConnectivityNetworkObserver` | `network/NetworkObserver.kt` | `isOnline()`, `online: StateFlow<Boolean>`, `awaitOnline(timeoutMs)`. Started in `OrbitApplication`. |
| `AdConfig`, `RemoteConfigRepository` | `remote/*` | Remote values with safe in-code defaults. |
| `ConsentManager` | `consent/ConsentManager.kt` | UMP: `canRequestAds`, `privacyOptionsRequired`, `formShowing`, `gather(activity)`, `showPrivacyOptions(activity)`. |
| `AdGate` / `AdGateRules` | `ads/AdGate.kt` | The ONE place that decides "may we request/show kind X (placement) now?" (premium, remote flags, ad unit id, consent, online, SDK ready). |
| `InterstitialAdManager` + `InterstitialCapping` | `ads/` | preload, 1h expiry, 10s load timeout, every-N-actions + min-interval capping, `showThen` always continues. |
| `AppOpenAdManager` | `ads/` | Off by default; never on cold start; only after `app_open_min_background_seconds`; only after onboarding is done. |
| `NativeAdPool`, `NativeAdSlot` | `ads/` | Pool hands each native ad out once; slot is design-system styled and collapses when unavailable. |
| `BannerAdManager`, `BannerAdSlot` | `ads/` | One shared adaptive banner. |
| `AdsController` | `ads/AdsController.kt` | Startup steps, resume-on-reconnect, premium clean-up, UI-facing API. |
| `ActivityTracker` | `ads/ActivityTracker.kt` | Weak current-Activity + foreground/background; no singleton holds an Activity. |

Rules enforced in code: premium users get no ads and no consent prompt; no request while `canRequestAds == false`; no
request while offline (already loaded ads may still render); children-directed flags are NOT set; ad slots never overlap
content; each native ad shows the "Ad" pill and an "Advertisement" semantic label.

## Startup flow (`core/startup`)

`InitializationHelper` steps (each with a timeout; only the database is critical):
`LOAD_SETTINGS` -> `WARM_DATABASE` -> `RECONCILE_REMINDERS` -> `LOCALE_READY` -> `CHECK_NETWORK` -> `CONSENT` (UMP,
6s info update, skipped for premium/offline/already usable) -> `REMOTE_CONFIG` (<= 7s) -> `MOBILE_ADS` (<= 7s, skipped for
premium/offline/no consent/ads off) -> `PRELOAD_ADS` (splash interstitial if the flow + flags allow, Language native on
first run or Home native + banner on later runs, app-open). It runs in the application scope, so work that outlives the
splash is not cancelled.

`SplashViewModel` + `SplashProgressRules`: online and not premium = long path (bar paced over the hard cap, waits for the
splash ad to load/fail, then jumps to 100%); offline or premium = short path (~2.2s). Never finishes before 2.2s and never
later than `splash_max_wait_seconds + 3` (time spent behind the consent form is not counted). `SplashUiState.showingAd` is
true while the splash interstitial is shown: **render a short overlay with `R.string.ads_loading`** while it is true.

`OnboardingFlowHelper` honours the remote JSON (`FlowConfigParser`), has the `SPLASH_INTERSTITIAL` step and keeps the
canonical order language -> onboarding -> premium -> home. When the network returns after an offline start, `AdsController`
re-runs consent, remote config, SDK init and the preloads by itself.

## Exact call sites for the lead

All composables resolve their dependencies with Koin (`koinInject`), so no ViewModel changes are needed.

1. **Language screen** (done: `LanguageAdBar` in `LanguageScreen`) - rect banner or large native in the bottom bar:
   ```kotlin
   NativeAdSlot(NativePlacement.LANGUAGE, style = NativeAdStyle.Large)           // below the list, >= 16dp away from Done
   val ads = koinInject<AdsController>()
   LaunchedEffect(Unit) { if (fromOnboarding) ads.preloadNative(NativePlacement.ONBOARDING) }
   ```
2. **Onboarding pages** - one Compact native under the text, not adjacent to Next/Done:
   `NativeAdSlot(NativePlacement.ONBOARDING, style = NativeAdStyle.Compact)` (each page gets its own ad from the pool).
3. **Exit sheet** - `NativeAdSlot(NativePlacement.EXIT, style = NativeAdStyle.Large)` inside the sheet, not next to Exit/Stay.
4. **Home** - one `NativeAdSlot(NativePlacement.HOME, style = NativeAdStyle.Compact)` between sections, after the 3rd card.
5. **Inner banner (shell)** - ONE instance in the app shell directly above the floating bottom bar:
   ```kotlin
   when (currentTab) {            // only these four routes; nothing on Splash/Premium/Feedback/Language/Onboarding/editors
       Home -> BannerAdSlot(BannerPlacement.HOME)
       Tasks -> BannerAdSlot(BannerPlacement.TASKS)
       Calendar -> BannerAdSlot(BannerPlacement.CALENDAR)
       More -> BannerAdSlot(BannerPlacement.MORE)
   }
   ```
   It sizes itself to the width, animates open/closed, and hides itself while the keyboard is open.
6. **Natural-transition interstitial** (task/note saved, Premium screen left, ...):
   ```kotlin
   val withInterstitial = rememberNaturalTransitionAd()   // core.ads
   // after the save succeeded:
   withInterstitial { nav.back() }                        // the lambda ALWAYS runs exactly once
   ```
   Capping: every `interstitial_nav_every_n_actions` calls AND `interstitial_min_interval_seconds` since the last
   full-screen ad. Never call it while the user is typing or mid-task.
7. **Splash overlay** - in `SplashScreen`, when `state.showingAd` show a scrim with `stringResource(R.string.ads_loading)`.
8. **Premium screen "no internet"** - before calling billing:
   ```kotlin
   val network = koinInject<NetworkObserver>()
   if (!network.isOnline()) { Toast.makeText(context, R.string.ads_no_internet, Toast.LENGTH_SHORT).show(); return }
   ```
9. **Settings "Privacy options"** - already added in `SettingsScreen` (visible only while UMP reports it as required).
10. **App-open** - nothing to wire. Call `koinInject<AdsController>().suppressAppOpenOnce()` before deliberately leaving
    the app for a long time (e.g. a purchase flow) if you want to skip the next app-open ad.
11. **Premium purchase** - when `SettingsStore.setPremium(true)` runs, `AdsController` observes it and destroys every
    loaded ad (banner, native pool, interstitial, app-open); slots collapse by themselves.

## Testing checklist

* Debug build shows Google test ads (the sample units always fill). Offline: slots collapse, no request is made, toggling
  airplane mode off resumes loading without a restart.
* EEA test: add `ConsentDebugSettings` temporarily in `ConsentManager.gather` (device hash from logcat) to see the form.
* `./gradlew bundleRelease` must fail (message from `verifyReleaseAdIds`) while the release `ad_ids.xml` holds the placeholder.
* Unit tests: `AdConfigTest`, `AdGateRulesTest`, `InterstitialCappingTest`, `FlowConfigParserTest`,
  `SplashProgressRulesTest`, `NetworkObserverTest`, `OnboardingFlowHelperTest`. The Language ad type is covered by `AdConfigTest` and `AdGateRulesTest`.

## Copy that still says "no ads / works offline only" (outside this task's files)

README, `startup_footer` ("Private. Offline. Yours."), the Premium strings, and Onboarding page 4 should be reviewed by the
lead. Privacy and About copy (`privacy_*`, `about_description` in `strings_backup.xml`) was rewritten in en/ur/es; the other
locales had those keys removed so they fall back to the truthful English text until re-translated.
