# Inactive OTP reference

These files preserve the earlier OTP design and implementation at the user's request. They are outside Android source sets, are not compiled into the app, and cannot be reached through navigation.

The previous `functions/` backend is also inactive: the app no longer depends on Firebase Functions or calls its endpoints, and its deployment entry was removed from the root `firebase.json`. No remote functions were deployed or deleted during this change. Do not deploy the old backend for password recovery.

Use the root `RECOVERY_SETUP.md` for the active Firebase Authentication password-reset email link flow. No custom SMTP account, OTP, or in-app new-password screen is needed.
