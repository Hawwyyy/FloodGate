# Firebase password-reset email links

## Active flow

Sign In → Forgot Password? → enter email → Send Reset Link → Check Your Email → open Firebase's link in the inbox → reset the password on Firebase's hosted page → return to the app and Sign In.

The project is Kotlin/Jetpack Compose, so the existing single-Activity architecture is retained. No XML screens or extra Activities are necessary. The confirmation screen uses the existing email icon, Inter typography, blue/white style, and buttons. The email field keeps the same gray border as Sign In/Sign Up; invalid fields use the existing error styling.

## Files

- `app/src/main/java/com/example/floodgate/ui/auth/RecoveryScreen.kt`: Forgot Password and Check Your Email UI, masked address, loading indicators, back controls, and cooldown display.
- `app/src/main/java/com/example/floodgate/ui/auth/RecoveryViewModel.kt`: trims/validates with `Patterns.EMAIL_ADDRESS`, prevents duplicate submissions, retains state during configuration changes, and enforces a 45-second resend cooldown.
- `app/src/main/java/com/example/floodgate/data/auth/PasswordResetRepository.kt`: calls `FirebaseAuth.getInstance().sendPasswordResetEmail(email)` and maps failures to friendly resource messages. Only Firebase Task success opens confirmation. No OTPs or Realtime Database writes.
- `app/src/main/java/com/example/floodgate/ui/auth/FloodGateAuthApp.kt`: existing Forgot Password navigation. Back arrow, system Back, and Back to Sign In close the recovery route and reset its state. There is no extra Activity stack to clear and no path back to OTP.
- `app/src/main/res/values/strings.xml`: screen text, error messages, resend notice and `00:45` countdown format.
- `app/src/androidTest/java/com/example/floodgate/RecoveryFlowInstrumentedTest.kt`: validation, masking, duplicate requests, error mapping, cooldown, resend success/failure, navigation, recreation and responsive-layout tests.
- `archive/otp/`: old OTP design/code/tests preserved outside Android source sets. Not visible or runnable in the app.
- `app/build.gradle.kts`, `firebase.json`: unused Functions dependency and old OTP backend deployment entry removed. Existing Auth, Database and App Check setup remains intact.

## Firebase Console setup

1. Open the Firebase project used by the existing `google-services.json` (`floodgate-c7bb3`).
2. Authentication → Sign-in method: ensure Email/Password is enabled.
3. Authentication → Templates → Password reset: customize your sender name and reset email template. Keep Firebase's default hosted action handler unless you intentionally provide your own working handler.
4. Test with an existing Email/Password account whose inbox you control. The app does not check whether an account exists or bypass Firebase's email-enumeration protection.

No SMTP credentials, Cloud Functions deployment, new database rules, Dynamic Links, or new-password screen are required for this flow. Existing App Check enforcement settings are respected; this change does not disable security settings in Firebase Console. If App Check is enforced for Authentication, configure your debug token/release Play Integrity as required by your existing setup.

Firebase can return success for an unregistered address when email-enumeration protection is enabled, without sending an email. The Check Your Email screen acknowledges Firebase's successful request response; it does not prove inbox delivery or that the password has already changed. Do not disable this protection or add account-lookup calls to expose registration status. See [FirebaseAuth password reset API](https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuth#sendPasswordResetEmail(java.lang.String)) and [Firebase user management](https://firebase.google.com/docs/auth/android/manage-users#send_a_password_reset_email).

## Behavior and limitations

- Blank email: “Please enter your email address.” Invalid email: “Please enter a valid email address.” No Firebase call until validation passes.
- All three authentication forms share `AuthValidator.validateEmail`: trim, Android `Patterns.EMAIL_ADDRESS`, length/dot/domain-label checks, and recognized domain endings from the IANA root-zone snapshot in `EmailTopLevelDomains.kt`. For example, `dhuaiduad@sjadad.sdaju` is rejected before Firebase is called. Refresh the snapshot from its documented IANA URL before releases so new domain endings are not blocked. This works offline and allows legitimate non-`.com` endings; it does not prove that a domain or mailbox exists, nor that the user owns it.
- During initial send: field/button disabled and spinner displayed. Failure restores the button and shows a friendly message, not a raw Firebase exception.
- Confirmation masks `harrison@gmail.com` as `h******@gmail.com` and never displays the full address.
- Resend uses the same validated address and API. Cooldown starts on initial success, on each resend attempt, and again on resend success. Failed resends remain retryable after cooldown. Firebase also enforces its own server-side request limits; the client countdown is not a security boundary.
- Resend success displays “Password reset email sent again.” Back remains usable while sending; a late callback cannot reopen a closed recovery screen.
- Rotation retains the ViewModel state. Process death starts recovery at the email form rather than persisting a possibly outdated success state. No passwords, action links or reset codes are stored in the app or database.
- Reset happens in the email link's hosted web page. Returning to the app does not automatically claim success or sign the user in; they use their new password on Sign In.

## Verification

Verified locally: debug/release builds passed, all 6 local unit tests passed, and all 27 Android emulator tests passed (13 recovery-specific tests). Visually reviewed the Forgot Password and Check Your Email screens, including a 320dp-wide layout with 1.5× text. The active Android sources contain no OTP repository calls or code-entry routes.

Build commands from the repository root:

```sh
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

The Android tests inject controlled responses to exercise the UI and Firebase Task adapter without sending real mail, changing real accounts, or clearing the user's session. They do not simulate OTP codes. A dedicated named Firebase instance is used for the Sign In navigation test so it cannot modify the user's existing authentication session.

Manual inbox acceptance test:

1. Tap Forgot Password from Sign In; check blank and malformed email errors.
2. Enter a registered address with leading/trailing spaces and tap Send Reset Link once.
3. Check the loading state and masked Check Your Email screen.
4. Confirm the message arrives (also check Spam/Junk). Open the reset link and set a new password on Firebase's hosted page.
5. Return to the app, tap Back to Sign In, and sign in with the new password.
6. Request another reset, wait for the cooldown, then use Resend Email and check the notice/message.
7. Check network failure and system Back behavior. If the emulator cannot resolve Firebase hostnames, restart it with working DNS; this is separate from password-reset logic.

Live inbox delivery and changing a real user's password are intentionally left for you to verify with your own account. No production reset emails or password changes are performed by the automated tests.
