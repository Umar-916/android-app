# ORBIT Planner (ORBIT Offline)

Offline-first personal and professional organizer for Android: tasks, projects, notes, calendar, habits, goals, analytics.
No account and no server: all user data lives in a local Room (SQLite) database and never leaves the device. The app works fully offline; the INTERNET permission is used only for Google AdMob ads, the UMP consent form and Firebase Remote Config (see docs/ADS_INTEGRATION.md and docs/REMOTE_CONFIG.md). Debug builds show Google TEST ads only; ad ids live in code, not Gradle: debug ids are in `core/ads/AdIds.kt` and `src/debug/res/values/ad_ids.xml`; paste the real release ids into `AdIds.kt` (RELEASE_* constants) and `src/release/res/values/ad_ids.xml` — release builds refuse to build (`verifyReleaseAdIds`) while they are placeholders or Google sample ids.

## Run
1. Open the folder in Android Studio (or use the JBR as `JAVA_HOME`).
2. Phone with USB debugging, then:
   ```bash
   ./gradlew :app:installDebug
   ```
3. Debug APK: `app/build/outputs/apk/debug/ORBIT-Planner-v1.0.0-debug.apk`
4. Unit tests: `./gradlew :app:testDebugUnitTest`
5. Instrumented tests (device required): `./gradlew :app:connectedDebugAndroidTest`

## Signed release
Create `keystore.properties` in the project root (git-ignored):
```
storeFile=orbit-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```
Create the key once with `keytool -genkeypair -v -keystore orbit-release.jks -alias orbit -keyalg RSA -keysize 2048 -validity 10000`, then:
```bash
./gradlew :app:assembleRelease   # APK
./gradlew :app:bundleRelease     # AAB for Play
```
Keep the keystore backed up; losing it means you cannot update the app.

## Architecture
Stack follows PrankHub: Navigation 3, Koin, Room, DataStore, WorkManager, Compose Material 3.
`core/` (database, repository, datastore, designsystem, navigation, notifications, backup, common) and `feature/` (onboarding, home, tasks, projects, calendar, notes, habits, goals, analytics, search, settings, more). MVVM, immutable UI state, repositories for all writes.

## Database decisions
- Dates are epoch-day Ints (no time zone), times are minute-of-day, instants are epoch millis.
- UUID string primary keys. Foreign keys: subtasks/habit logs cascade; project/folder deletion sets child to NULL; deleting tasks with a project is an explicit user choice.
- Unique indexes: (habitId, date) and (seriesId, dueDate) prevent duplicate check-ins and duplicate recurring occurrences.
- No destructive migration. Schemas exported to `app/schemas`; add a Migration and a test in `OrbitMigrationTest` on each version bump.
- Settings use Preferences DataStore (typed wrapper), same as PrankHub, not Proto DataStore.

## Reminders
WorkManager unique work per task; reconciled at app start, after reboot, app update, clock and time-zone change. Delivery is approximate (Doze/battery saver can delay it); exact alarms are deliberately not used.

## Backup
Versioned JSON via the Storage Access Framework, validated before restore, restored in one transaction. CSV export for tasks and habits. Backups are NOT encrypted.

## Known limitations
- Premium (ORBIT Plus = no ads) is fully designed but billing is not configured (`AppConfig.PREMIUM_PRODUCT_ID` blank): screens honestly say purchases are not available. See docs/PREMIUM_INTEGRATION.md.
- Remote Config uses in-code defaults until Firebase credentials are set (docs/REMOTE_CONFIG.md).
- Feedback is hidden until `AppConfig.FEEDBACK_EMAIL` is set.
- Tab switching resets the previous tab's transient state (PrankHub-style navigation).
- Urdu and Spanish translations were machine-drafted and should get a native review.
