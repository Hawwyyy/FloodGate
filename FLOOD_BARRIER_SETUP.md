# Live FloodGate 01 dashboard

Home observes `/devices/floodgate01` using existing Firebase configuration, login and profiles. The earlier LED-demo Devices tab remains separate from this flood-barrier connection.

## Implementation

- `barrier/model/BarrierTelemetry.kt`: nullable validation, action definitions and repository contract. Unknown calibration stays Unknown, never an ADC-derived percentage.
- `barrier/model/FirebaseBarrierRepository.kt`: device, client connection, server offset and authentication listeners. Only writes an atomic `{barrier: DEPLOY|UNDEPLOY, id: UUID}` to `/devices/floodgate01/command`.
- `barrier/presenter/BarrierPresenter.kt`: freshness, latest 20 readings, pending writes, errors and device-reported feedback.
- `barrier/view/LiveBarrierScreen.kt`: Compose dashboard, confirmation dialogs, lifecycle and accessible chart. Authentication visuals and behavior are unchanged.

Each confirmed action gets a new UUID, including repeated actions. Firebase write success does not prove movement. A matching subsequent state means only “Device reports deployed/retracted”; the switch or automatic logic could have caused it. WEBSITE is displayed as Remote control.

Freshness uses updatedAt and Firebase server offset with a 15-second cutoff. The phone's connection is shown separately. Disconnected or stale values remain labeled last known; controls disable. Only advancing timestamps add valid calibrated readings. History stays in memory across Home/Devices switches and background/foreground, clears at sign-out, and may reset on Activity recreation.

Pending writes block duplicate clicks. After 15 seconds the UI explains they may complete after reconnection; timeout does not cancel them. Leaving or signing out also does not imply queued SDK writes are canceled. Listeners detach on stop, and session data is cleared on sign-out.

No live rules or firmware are changed. Use existing authenticated device reads and command-child writes. Do not deploy the older local demo rules over the website's working rules. Android never writes telemetry or device source. Do not embed device credentials in Android.

## Manual hardware verification

1. Login and compare percentage, raw ADC, status, outputs and timestamp with Firebase; malformed/missing fields show Unknown.
2. Confirm Deploy and Retract. Check the command pair, then separately observe reported state and physical movement.
3. Repeat Deploy: each action must have a different ID, with no clearing or NONE writes.
4. Toggle the physical switch; verify live outputs, barrier state and Physical switch source.
5. Hold water above 30% for five seconds; verify FLOOD CONFIRMED and automatic outputs/90°.
6. Manually turn OFF during high water: flood remains confirmed while outputs turn off and suppression is true. Drop water to <=30% to reset suppression. Falling water alone must not retract an active barrier.
7. Disconnect ESP32 Wi-Fi; after >15 seconds verify stale telemetry and disabled controls. Disconnect the phone separately, then reconnect. Command-only changes must not add chart readings or reset telemetry age.
8. Test permission denial in a safe test environment; verify errors and Retry. Disconnect during a write and verify pending-timeout wording and late completion.
9. Sign out; verify the existing sign-in page, cleared history and removed listeners. Sign in again for a new device session.

JVM tests use fake repositories. Emulator dashboard tests also use fake telemetry and do not operate hardware. Website synchronization, permissions, Wi-Fi and physical servo/output behavior require the checks above.
