# FloodGate onboarding assets

Implemented from the FloodGate Figma frames:

- Onboarding 2, `19:369` — Real-Time Monitoring
- Onboarding 3, `19:389` — Instant Alerts, Peace of Mind

The three alert icons and notification logo were downloaded from Figma. SVG originals are kept in
this directory; Android VectorDrawable conversions live in `app/src/main/res/drawable`.

The Figma progress images were replaced with equivalent Compose circles using the exact colors
`#566FEC` and `#D7DEF2`. Each visible dot has a larger touch target, exposes a selected state to
accessibility services, and navigates directly to its corresponding onboarding page.

Startup no longer advances after a timer. A tap anywhere on the splash screen continues to page 1
for users who have not completed the current onboarding version. Next advances pages 1 → 2 → 3;
Get Started completes onboarding and opens authentication. Back moves to the preceding onboarding
page. Signed-in users and users who completed the current version go to the app after tapping splash.
