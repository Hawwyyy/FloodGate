# ARCHIVED: old OTP backend — do not use for the active password-reset flow

## What is implemented

Forgot Password opens the Figma email screen (`100:481`). The app validates and trims the email, calls `sendRecoveryCode`, and opens the OTP screen (`89:141`) only after SMTP accepts the message. It displays a resend countdown, prevents duplicate requests, accepts/pastes six digits, and opens “Email Verified!” (`91:269`) only after `verifyRecoveryCode` succeeds. Failed requests show friendly errors. Back from the code screen returns to email entry; back from the verified screen returns to Sign In.

This is the requested **email verification portion**, not a completed password-reset mechanism. The success caption follows Figma, but this version does not change a password, sign in, mark a Firebase Auth user verified, or issue a reusable reset credential. Before adding a new-password screen, extend the backend to issue a short-lived, single-use, email-bound reset grant and consume it server-side when updating the Firebase Auth password. Never authorize password changes using the Android `VERIFIED` UI state.

## Files and responsibilities

- `app/src/main/java/com/example/floodgate/ui/auth/RecoveryScreen.kt`: three responsive Compose screens, shared header/icon, six-cell accessible input, Figma colors and dimensions. Existing Inter typography, primary buttons and email field are reused. Inter SemiBold currently uses the app’s existing synthetic weight because only its regular font is bundled.
- `ui/auth/RecoveryViewModel.kt`: loading, field validation, challenge lifetime, callbacks, resend cooldown and navigation state; retained across activity recreation. Process death intentionally discards the challenge instead of persisting codes.
- `data/auth/EmailOtpRepository.kt`: callable Firebase Functions adapter; no local code generation/comparison or raw exception messages.
- `ui/auth/FloodGateAuthApp.kt`: connects the existing Forgot Password link without changing sign-in/sign-up behavior.
- `res/drawable/recovery_email.xml`, `recovery_check.xml`: vector conversions of the actual Figma assets.
- `res/values/strings.xml`: labels and validation/service errors.
- `FloodGateApplication.kt`, `src/debug/.../RecoveryAppCheck.kt`, `src/release/.../RecoveryAppCheck.kt`, manifest and app Gradle file: App Check initialization and non-KTX dependencies. No package or architectural conversion.
- `functions/index.js`: callable endpoints, TLS SMTP transport, Firebase Admin transactions, hourly expired-record cleanup.
- `functions/otp-service.js`: cryptographically random OTPs, HMAC digests, email/IP limits, expiry and atomic single-use verification.
- `firebase.json`, `database.rules.json`: recovery functions configuration and server-only record access; existing user-profile rules preserved.

## Activation needed before real emails will work

No sender password is bundled, no production functions/rules have been deployed by this implementation, and no live email has been sent as part of testing.

1. In Firebase Console, select the app’s project **`floodgate-c7bb3`**. Confirm the Android app package is `com.example.floodgate` and Realtime Database is enabled. Copy its exact database URL; do not guess the region or database instance name.
2. Cloud Functions deployment requires the **Blaze billing plan**. Enabling billing is your choice; set budget alerts and review Firebase/SMTP provider pricing before proceeding. See [Firebase deployment prerequisites](https://firebase.google.com/docs/functions/get-started).
3. Set up an SMTP-capable email provider and a verified sender. Obtain its SMTP host, TLS port (465 or 587), username and password/app password. Configure the provider’s required domain authentication (SPF/DKIM/DMARC). Do not paste secret credentials into chat or commit them.
4. Create `functions/.env.floodgate-c7bb3` using `functions/.env.example` as the template. Fill `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_FROM` (verified sender), and `RECOVERY_DATABASE_URL`. This file is git-ignored. It must exist locally during deployment; the CLI passes these values to Cloud Functions.
5. Install/use Node.js **22** and the Firebase CLI, then run from the project root:

   ```sh
   npm --prefix functions ci
   firebase login
   firebase functions:secrets:set SMTP_PASSWORD --project floodgate-c7bb3
   firebase functions:secrets:set OTP_HMAC_KEY --project floodgate-c7bb3
   ```

   The first secret is the provider’s SMTP/app password. For the second, generate at least 32 random bytes in a password manager or using `openssl rand -hex 32` in your own terminal, then enter it at the secret prompt. Keep it private. Never place it in Android resources, Gradle files, source code or `.env.example`. Rotating this key invalidates outstanding challenges and resets hashed-key throttling buckets; do not rotate casually.

6. Register App Check before testing callable functions:

   - For Android Studio/debug: start the app, trigger a recovery request, find the local debug-provider token in Logcat, then register it in Firebase Console → App Check → Android app → Manage debug tokens. Keep it private; do not publish logs containing it. See [debug provider setup](https://firebase.google.com/docs/app-check/android/debug-provider).
   - For release: configure Play Integrity in Firebase/Google Play and register the release signing certificate SHA-256. Debug providers are excluded from release builds. Review Play Integrity app-distribution requirements for sideloaded builds. See [Play Integrity setup](https://firebase.google.com/docs/app-check/android/play-integrity-provider).
   - Both callable endpoints enforce App Check regardless of console enforcement settings. Do not disable this to bypass configuration errors. Existing Firebase Auth/database enforcement settings are not changed automatically.

7. After reviewing the configuration and approving any billing, deploy only this codebase and database rules:

   ```sh
   firebase deploy --only functions:recovery,database --project floodgate-c7bb3
   ```

   Endpoints use `us-central1`, matching `FirebaseEmailOtpRepository`. If changing region, update both. Do not run `firebase init` over the existing project configuration.

## Security and behavior

- Codes: six cryptographically random digits; ten-minute expiry, maximum five incorrect guesses per challenge, single-use consumption via Realtime Database transactions. Codes are never returned to the app, stored plaintext, or logged.
- Sending: minimum 60 seconds between messages and maximum five messages per email per hour. A shared per-IP budget permits 30 send/verify requests per hour. These are server limits, not just button timers. Legitimate users sharing one IP can hit this limit; review quotas before larger rollouts.
- HMAC-keyed email/IP identifiers are stored instead of raw addresses. Digests bind the email, challenge ID and code. Resend invalidates the preceding challenge. Failed deliveries keep rate limits intact. The SMTP provider necessarily receives the recipient and message.
- SMTP uses verified TLS certificates, timeouts, and ports 465/587 only. SMTP acceptance is not an inbox-delivery guarantee; check spam folders/provider delivery reports if needed.
- No Firebase account lookup is performed: the response does not disclose whether the address is registered. A valid recipient can verify email ownership without having an existing FloodGate account. Verification alone grants no account access.
- Database clients, including authenticated clients, cannot access `recoveryChallenges` or `recoveryRateLimits`; Admin SDK bypasses rules on the server. Hourly cleanup removes up to 500 records per collection per run after 24 hours. Expired codes are rejected immediately even before cleanup.
- App Check, email/IP limits and instance limits reduce abuse, but are not a hard spending cap. Monitor quotas and mail-provider sending limits before opening the feature to the public.
- The UUID transitive override pins CommonJS-compatible `11.1.1` to address an audit advisory in upstream Google SDK dependencies. Their call sites use the compatible `v4()` API. Re-evaluate/remove the override after upstream upgrades.

## Tests and verification commands

From the repository root:

```sh
npm --prefix functions test
npm --prefix functions run check
firebase emulators:exec --only database --project demo-floodgate-recovery --config firebase.emulators.json 'npm --prefix functions run test:database'
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
./gradlew :app:connectedDebugAndroidTest
```

The database integration test explicitly requires a local emulator and uses a `demo-` project; it never touches production. Android tests inject a test-only repository and do not send mail. Backend tests cover validation, expiry, guess limits, single-use/replay, resend invalidation, concurrent requests, SMTP failures and throttling; database tests check real transactions and denied client reads/writes. Android tests cover validation, loading/duplicate prevention, code paste/editing, wrong-code errors, verified-screen gating, stale callback isolation, expiry, and a small phone with larger text.

After deployment, manually test with an inbox you control: invalid email; actual delivery; wrong/incomplete code; resend after 60 seconds; old-code rejection; expiry after ten minutes; correct-code success; back navigation; offline/send failure; debug and signed release App Check. Live SMTP delivery and production App Check cannot be verified until your sender configuration is provided.

### Local verification completed

- Debug and release APK builds: passed.
- Existing Android unit tests: passed.
- Android emulator suite: all 21 tests passed, including seven new recovery tests.
- Backend logic suite: all 11 tests passed.
- Realtime Database emulator: concurrent send/single-use verification and denied-client access checks passed.
- Backend package audit after the UUID override: zero reported vulnerabilities.
- Visually inspected email, code, success, and 320dp-wide/1.5× text screenshots. These are test-rendered screens, not evidence of live mail delivery.
