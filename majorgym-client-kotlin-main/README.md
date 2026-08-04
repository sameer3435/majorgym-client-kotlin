# MajorGym Client — Kotlin + Jetpack Compose

Native Android rebuild of the Flutter client app, using the same stack as
the owner app (Kotlin, Jetpack Compose, no Navigation-Compose — just a
sealed `Screen` state, same as the owner app's `MainActivity`). Feature set
is unchanged from the Flutter version — nothing added, nothing removed:

1. **Scan to Update Membership** — scan a QR to join or renew. QR must
   contain JSON text, e.g.:
   ```json
   {"name":"Sameer","phone":"9876543210","id":"MG001","joiningDate":"2026-01-15","expiryDate":"2026-08-15"}
   ```
   Scanning it overwrites whatever profile is cached (a renewal QR just
   carries fresh dates). Field matching is case-insensitive, and expiry is
   computed from joining date + plan duration when the explicit expiry
   field isn't trustworthy — exactly like the Flutter version's
   `Member.fromQrJson`.

2. **Scan to Mark Attendance** — scan the gym's static QR. Any QR text
   works; a scan simply marks today present (once per day).

Home screen shows: Name, Phone, ID, Joining date, Renewal date, days
remaining before expiry, plus today's attendance Day / Status / Streak.

No login, no cloud backend, no SQL — everything is stored on-device via
`SharedPreferences` (the native equivalent of the Flutter app's
`shared_preferences` package).

## What's different from the owner app (and why)
- No Room/SQLite — this app only ever caches a single member record plus a
  60-day attendance map, so plain `SharedPreferences` (matching what the
  Flutter app used) is enough; there was never a need for a real database.
- No Wi-Fi sync, no backup/export, no photo storage — those are owner-app
  features for managing many members; this app only manages one member's
  own view of their own membership.
- QR **scanning** uses CameraX + ML Kit barcode-scanning (the same
  ML Kit engine `mobile_scanner` uses under the hood on Android), since the
  owner app only ever *generates* QR codes (via ZXing) and never scans them.

## Building it via GitHub Actions (no local Android Studio needed)
1. Push this repo's contents as-is.
2. Go to the repo's **Actions** tab — the workflow runs automatically.
3. It builds both `app-debug.apk` and `app-release-unsigned.apk`.
4. Download the `majorgym-client-kotlin-android` artifact, unzip it, and
   install the debug APK on your phone (the release one is unsigned and
   won't install as-is — see below).

## Building it locally instead
1. Install **Android Studio** (bundles JDK 17).
2. Open Android Studio → **Open** → select this folder.
3. Android Studio will detect there's no Gradle wrapper jar and offer to
   generate one — click **OK** (needs internet once).
4. Wait for Gradle Sync, then plug in a phone (USB debugging on) or use an
   emulator, and click **Run ▶**.

For a signed release build (recommended before sharing widely):
**Build → Generate Signed Bundle / APK → APK**, then follow the wizard to
create a signing key.

## Notes
- `minSdk` is 26 (Android 8.0+), same floor as the owner app.
- No `google-services.json`, API keys, or backend of any kind are required.
- Package name is `com.majorgym.client` (distinct from the owner app's
  `com.majorgym.app`) so both apps can be installed on the same phone at
  once.
