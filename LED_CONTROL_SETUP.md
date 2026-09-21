# External LED demonstration

The existing Compose dashboard contains the MVP LED card. Authentication, the other dashboard cards, and `/devices/floodgate01` are unchanged. Other dashboard readings are still demo data.

## Firebase setup

1. Use the existing project `floodgate-c7bb3`. Create a dedicated Email/Password Firebase Auth account for the ESP32 in the console. Do not use a team member's account.
2. Copy its UID and, as an administrator in the database console, set `/eldroidDeviceUids/DEVICE_UID` to boolean `true`. Clients cannot edit this allowlist. This grants only the telemetry permissions defined in the rules. Remove the entry to revoke the board.
3. Merge the `eldroid` rules from `database.rules.json` into the live rules, preserving any live-only paths such as `/devices/floodgate01`. Do not blindly deploy the entire local rules file if live rules differ. The app users can write only commands; the authorized board can write telemetry. Anonymous users cannot send commands. Reads require authentication.
4. Optionally initialize only `/eldroid/ledControl/command` to string `OFF`. Leave actualState and timestamps absent until hardware reports them; missing telemetry displays UNKNOWN/UNAVAILABLE.
5. If Realtime Database App Check enforcement is enabled, this supplied ESP32 library does not provide an App Check token. A supported device gateway/custom App Check integration is required; do not make rules public to bypass it. Android debug installations also need their App Check debug token registered when enforcement is enabled.

## ESP32

Open `firmware/ExternalLed/ExternalLed.ino` in Arduino IDE. Install the library used by the supplied sketch, **Firebase Arduino Client Library for ESP8266 and ESP32** by Mobizt (4.4.17), and an ESP32 board package compatible with it. This legacy library is deprecated upstream; retained here for compatibility with your provided code. See https://github.com/mobizt/Firebase-ESP-Client . Firmware compilation and flashing require your board/toolchain and have not been verified locally.

Copy `secrets.example.h` to `secrets.h` in the sketch directory and fill in Wi-Fi, the existing project's API key, and the dedicated device account credentials. `secrets.h` is ignored by Git. Do not use an admin service-account key on the board.

Connect GPIO 23 through a suitable current-limiting resistor (for example 330 ohms) to the LED anode, and LED cathode to ESP32 GND. Use an active-high external LED. Never connect mains or a high-current load to a GPIO. Select the board and port, then upload.

The firmware reads back the GPIO output, rather than echoing a command variable. This confirms the output level but **cannot detect a failed/disconnected LED or prove light emission**. For physical confirmation, connect an appropriate 3.3V digital light/current feedback circuit and set `FEEDBACK_PIN` to its input pin; HIGH must mean light/current detected. ActualState then comes from that sensor. Calibrate and verify the sensor independently.

The board polls commands every 500 ms and atomically reports actualState, a heartbeat counter, and a server timestamp every 2 seconds. Failed reports are retried by the next heartbeat. Invalid commands leave the output unchanged. The output starts OFF and adopts the saved command after reconnecting.

## Data flow and verification

Android writes only `/eldroid/ledControl/command` (`ON` or `OFF`). A successful write shows a short confirmation; it never changes actualState in the UI. The ESP32 applies the command and writes `/eldroid/ledControl/actualState`, `/eldroid/ledControl/device/lastUpdate`, and `/eldroid/ledControl/device/heartbeat`. A realtime listener updates the card.

The Presenter checks lastUpdate every second using Firebase's server time offset. Missing/invalid timestamps mean UNAVAILABLE; age <=10 seconds means ONLINE; older timestamps mean OFFLINE. Offline telemetry is explicitly labeled Last Known LED. Offline commands can be queued by Firebase; the UI keeps duplicate submissions disabled and explains pending delivery after 15 seconds. Reopening the screen can restart observation; a queued command may still arrive later.

Test with an authenticated app user: send ON and verify the physical LED and subsequent actualState; repeat OFF. Disconnect board Wi-Fi/power and wait >10 seconds for OFFLINE, reconnect and confirm ONLINE. Test missing/invalid values using the administrator console or emulator (client rules intentionally reject invalid writes). Test permission denial and phone connectivity loss. Android build/unit tests do not prove the hardware circuit or live Firebase permissions work.

MVP: `ledcontrol/model` owns Firebase listeners/writes and typed snapshot parsing; `ledcontrol/presenter` owns status timing, pending state and view events; `ledcontrol/view` renders Compose UI and forwards lifecycle/button events. Listeners and timers stop when the dashboard stops.
