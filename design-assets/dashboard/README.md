# FloodGate dashboard assets

Source: [FloodGate dashboard, frame 95:377](https://www.figma.com/design/6SoTurhUkEXYi0PQcyT6XE/FloodGate?node-id=95-377).
Downloaded from the Figma design-context exports on 2026-09-10.

- `app/src/main/res/drawable-nodpi/dashboard_logo.png`: original transparent logo, 1200 × 847.
- `app/src/main/res/drawable-nodpi/dashboard_barrier.png`: original barrier photo, 1367 × 1150.
- SVG originals are kept in this directory. Android VectorDrawable equivalents are in
  `app/src/main/res/drawable/dashboard_*.xml`, generated with Android SDK `Svg2Vector` using
  `tools/ConvertDashboardSvg.java`. No replacement icon library is used.
- Figma's mock phone status bar is intentionally replaced by Android's native status bar.
- The Devices icon uses a direct `SVG_STRING` export of `I96:436;95:422`, because the
  design-context download incorrectly returned the main component's Home icon instead of its override.

## Implementation

`ui/dashboard/DashboardScreen.kt` uses the existing Compose theme and Inter family. Colors specific
to this screen are mapped in `DashboardTokens.kt`; text is in `dashboard_strings.xml`.
The activity surface follows its bound `surface/primary` variable (`#ECEDEE`), not the conflicting
generated fallback `#F5F5F5`. Other tokens follow the frame's resolved design-system variables.
Medium/semi-bold text uses the existing project's synthesized Inter weights.
Android's SVG converter omits the online indicator's SVG Gaussian glow; its original vector
geometry and green indicator background are retained. Card elevation approximates Figma's shadow.

The barrier image keeps the Figma top-aligned crop. Content scrolls; metric cards stack on narrow
phones or at larger font scales. The bottom bar remains available independently of vertical scrolling;
at larger font scales its tabs scroll horizontally to preserve full labels rather than shrinking text.

## Data and navigation

Successful email/password authentication and an existing Firebase session both open this dashboard.
Registration still returns to Sign In. Profile exposes the authenticated email and Sign Out.
The activity stack is unchanged: Android Back exits the authenticated flow instead of reopening
the sign-in form.

**Readings, alerts, deployment state, and location are explicitly demo data from Figma.** No device
integration exists yet. Deploy/Retract explain that no command was sent. Devices, Notification,
location, and activity show informative dialogs, not invented backend functionality.

## Verification

Build with `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`.
`DashboardInstrumentedTest` covers login success/failure, saved sessions, back handling,
state restoration, sign-out, safe demo controls, and small-phone/large-font layouts.
Its fake authentication repository never creates accounts or signs out a real Firebase user.
