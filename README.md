# FloodGate Android

FloodGate is a Kotlin/Jetpack Compose Android MVP for flood-barrier monitoring. The current build includes the Figma-based splash and onboarding flow, email/password authentication, Firebase Realtime Database user profiles, Firebase password-reset email links, and a responsive dashboard that currently displays design/demo monitoring data.

## App flow

1. Tap anywhere on the splash screen.
2. New installations show three onboarding pages. **Next**, **Get Started**, Back, and the three progress dots are functional.
3. Sign in with an existing Firebase Email/Password account, create a new account, or request a password-reset email.
4. Registration stores only the user's first name, last name, email, and creation timestamp under `users/{uid}` in Realtime Database. Passwords are never stored in the database. Registration returns to Sign In.
5. A successful sign-in, or an existing Firebase session, opens the dashboard.

The dashboard's water level, barrier state, activity, and location are currently **sample data from the Figma design**. No physical flood-barrier device or live sensor backend is connected yet.

All active screens support small phones. Onboarding scales as one centered design surface, authentication and recovery pages scroll vertically, long controls can grow or wrap, and dashboard cards stack when horizontal space or text size requires it. The automated UI suite includes 320×640dp coverage and enlarged text checks.

## Technology

- Kotlin and Jetpack Compose
- Single-Activity architecture
- Android Gradle Plugin 8.3.2 and Gradle 8.4
- Compile/target SDK 34; minimum SDK 24
- Firebase Authentication: Email/Password and password-reset email links
- Firebase Realtime Database: basic user profiles
- Firebase App Check: debug provider in debug builds, Play Integrity in release builds

## Prerequisites

Install the following before opening the project:

- Android Studio Iguana (2023.2.1) or newer
- JDK 17 (Android Studio's bundled JDK is recommended)
- Android SDK Platform 34 and current Android SDK build tools
- An Android emulator or physical device running Android 7.0/API 24 or newer
- Git

For real authentication tests, the Firebase project must have Email/Password authentication and Realtime Database enabled.

## Clone and run

```bash
git clone https://github.com/Hawwyyy/FloodGate.git
cd FloodGate
```

1. Open the repository root in Android Studio.
2. Open **Settings/Preferences → Build, Execution, Deployment → Build Tools → Gradle** and set **Gradle JDK** to Android Studio's bundled JDK/JBR 17. Do not enter another teammate's absolute Java path.
3. Allow Gradle Sync to finish. Android Studio creates your machine-specific `local.properties` file automatically.
4. Confirm that `app/google-services.json` exists. The repository currently contains the FloodGate Firebase client configuration. If your team uses a different Firebase project, download that project's Android configuration and replace this file locally.
5. Start an API 34 emulator, or connect a physical Android device with Developer options and USB debugging enabled.
6. Select the `app` run configuration and press **Run**.

Command-line build:

```bash
./gradlew :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Firebase setup

### Authentication

In Firebase Console, open the project referenced by `app/google-services.json`, then:

1. Go to **Authentication → Sign-in method**.
2. Enable **Email/Password**.
3. Go to **Authentication → Templates → Password reset** to customize the reset-email sender and message if needed.

The app uses Firebase's hosted password-reset link. It does not generate OTP codes and does not contain an in-app new-password form.

### Realtime Database

Create a Realtime Database for the Firebase project, then deploy the included user-profile rules:

```bash
npm install -g firebase-tools
firebase login
firebase use YOUR_FIREBASE_PROJECT_ID
firebase deploy --only database
```

The active rules are in `database.rules.json`. Each authenticated user can read and write only their own `users/{uid}` profile.

### App Check on another emulator or phone

Debug builds use Firebase's App Check debug provider. If App Check enforcement is enabled, every teammate/device needs its own registered debug token:

1. Run the debug app on that emulator or phone.
2. In Android Studio Logcat, search for `DebugAppCheckProvider` or `Enter this debug secret`.
3. Copy the displayed token.
4. In Firebase Console, go to **App Check → Apps → Manage debug tokens** and register it.
5. Restart the app and retry the Firebase request.

Release builds use Play Integrity. A locally built release APK is unsigned unless a signing configuration is added, and Play Integrity normally requires the app to be registered and distributed using the expected signing identity. Use the debug build for normal team testing.

## Testing on a fresh device

To see the complete first-launch experience, install the app on a device where it has not run before. To repeat onboarding later, clear the app's local data:

```bash
adb shell pm clear com.example.floodgate
```

Clearing local app data resets onboarding and the local Firebase session; it does not delete Firebase accounts or database profiles.

Suggested acceptance test:

1. Tap the splash screen and verify all three onboarding pages, progress dots, and buttons.
2. Create an account using a unique email and a password containing at least eight characters, uppercase, lowercase, a number, and a special character.
3. Verify registration returns to Sign In and displays the success notice.
4. Sign in and verify the dashboard opens.
5. Sign out from the Profile action and sign in again to verify authentication persistence.
6. Use **Forgot Password**, submit a registered email, open the Firebase email link, reset the password, and return to Sign In.
7. Wait for the 45-second cooldown and verify **Resend Email** when testing password recovery.

## Automated verification

Build debug/release variants and run local tests:

```bash
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

With an emulator or device running, execute the Android UI tests:

```bash
./gradlew :app:connectedDebugAndroidTest
```

The UI tests use controlled repositories for authentication and password-recovery behavior. They do not create production accounts, send real reset emails, or issue real device-control commands.

## Troubleshooting

- **`Unresolved reference: ComponentActivity` or `setContent`:** run Gradle Sync, confirm JDK 17 is selected, and rebuild the project.
- **`Value ... given for org.gradle.java.home is invalid`:** pull the latest `main` branch. The project does not set a shared Java path. Select the bundled JDK/JBR 17 under Android Studio's Gradle settings. Also remove any stale `org.gradle.java.home` entry from the user's `~/.gradle/gradle.properties`, if one exists there.
- **`Unable to connect`:** check the emulator/device internet connection, confirm Email/Password is enabled, and verify the Firebase configuration belongs to an active project.
- **Firebase requests fail only on a teammate's device:** register that device's App Check debug token using the steps above.
- **Profile save fails after account creation:** confirm Realtime Database exists and deploy `database.rules.json`.
- **Password-reset email is missing:** check Spam/Junk, verify the account uses Email/Password, review the Firebase Authentication template, and remember that Firebase may return success for an unknown address when email-enumeration protection is enabled.
- **AVD terminates:** cold boot or wipe that AVD, confirm sufficient disk/RAM is available, update Emulator tools, or create a new API 34 virtual device.

## Repository notes

- `RECOVERY_SETUP.md` contains detailed password-reset behavior and acceptance-test notes.
- `design-assets/` preserves original Figma SVG exports used to produce Android vector resources.
- `archive/otp/` and the root `functions/` directory preserve the earlier OTP experiment for reference only. They are not compiled, called, or deployed by the active application. Do not deploy them for the current password-reset flow.
- Never commit `local.properties`, service-account keys, SMTP passwords, keystores, or `.env` files. The existing `.gitignore` excludes local build output, Functions dependencies, and local secret files.
