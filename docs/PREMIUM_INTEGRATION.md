# Premium (ORBIT Plus) integration

ORBIT ships **without** purchases. The premium screens (`feature/premium`) are complete and honest: while no real
billing provider is installed they say "Purchases are not available yet" and only offer "Maybe later". Nothing that
looks like a working Buy button is shown, no price is rendered, and no essential feature is ever gated.

## Pieces

| Piece | Where | Role |
| --- | --- | --- |
| `BillingProvider` | `feature/premium/BillingProvider.kt` | The only door through which a purchase becomes entitlement |
| `UnavailableBillingProvider` | same file | Default; `isAvailable = false`, no price, never purchased |
| `AppConfig.PREMIUM_PRODUCT_ID` | `core/common/Helpers.kt` | Play Console product id; blank = purchases not configured |
| `SettingsStore.isPremium` / `setPremium(true)` | `core/datastore/SettingsStore.kt` | Persisted entitlement flag |
| `isPremiumUser(settings)` | `feature/premium/PremiumEntitlement.kt` | Use it to hide upgrade prompts |
| `PremiumUpgradeCard` | `feature/premium/PremiumUpgradeCard.kt` | Settings entry (hide it when premium) |
| `PremiumClosePolicy` | `feature/premium/PremiumClosePolicy.kt` | Close button / back rules (unit tested) |
| Koin binding | `startupFeatureModule` in `core/startup/StartupModule.kt` | `single<BillingProvider> { ... }` |

## Plugging in Google Play Billing

1. Add the Play Billing dependency (`com.android.billingclient:billing-ktx`) and, in Play Console, create the
   product. Put its id into `AppConfig.PREMIUM_PRODUCT_ID`.
2. Implement `BillingProvider` (for example `PlayBillingProvider(context)`):
   * connect a `BillingClient`; set `isAvailable = true` only once it is connected and the product details for
     `AppConfig.PREMIUM_PRODUCT_ID` were fetched;
   * publish the localized price (`ProductDetails.oneTimePurchaseOfferDetails.formattedPrice` or the subscription
     phase price) through `price`; keep it `null` until known, the UI then shows no price at all;
   * `purchase(activity)` launches `launchBillingFlow`; after the purchase is verified and acknowledged set
     `isPurchased` to `true`;
   * `restore()` calls `queryPurchasesAsync` and updates `isPurchased` (called automatically when the premium
     screen opens and from the "Restore purchase" button, which only appears when a purchase is possible).
3. Replace the binding in `startupFeatureModule`:
   `single<BillingProvider> { PlayBillingProvider(androidContext()) }`.
4. Add the `com.android.vending.BILLING` permission (and `INTERNET` if your billing library needs it) to the
   manifest. This is a deliberate product decision: the app is currently fully offline.

That is all. The three art pages automatically switch from "not available" to the Continue button with the nudging
arrow once `isAvailable` is true and `price` is known (`PremiumUiState.canPurchase`). When `isPurchased` becomes true
the screen calls `SettingsStore.setPremium(true)` and closes; Settings then hides `PremiumUpgradeCard` through
`isPremiumUser`.

## Close behaviour

`PremiumClosePolicy(homeReached, mode, billingAvailable)`:

* after Home was reached (`fromOnboarding = false`): back always works, close button at the start;
* first run, modes: `0` hard paywall, `1` close at the end immediately, `2` close at the end after 3 s (default),
  `3` close at the start after 3 s;
* a hard paywall (mode 0) never traps the user while billing is unavailable.

## Variants

`AppDestination.Premium(variant, fromOnboarding)`: `1` Aurora (themed), `2` Nova (dark space, hides system bars),
`3` Eclipse (full-bleed gradient). The first-run flow picks the variant in `OnboardingFlowHelper.configFor`;
in-app entries use variant 1.
