#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "secrets.h"

// GPIO readback confirms output voltage, not that an LED emits light.
// For optical confirmation attach a suitable 3.3V digital sensor and set this pin.
const int LED_PIN = 23;
const int FEEDBACK_PIN = -1;
const char *ROOT = "/eldroid/ledControl";
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;
unsigned long lastPoll = 0, lastReport = 0, lastReconnect = 0;
unsigned long heartbeat = 0;

void reportState() {
  FirebaseJson report;
  bool measured = digitalRead(FEEDBACK_PIN >= 0 ? FEEDBACK_PIN : LED_PIN) == HIGH;
  report.set("actualState", measured);
  report.set("device/heartbeat", ++heartbeat);
  report.set("device/lastUpdate/.sv", "timestamp");
  // One atomic update keeps the state and its freshness timestamp together.
  if (!Firebase.RTDB.updateNode(&fbdo, ROOT, &report)) {
    Serial.println("Telemetry failed; will retry on next heartbeat.");
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);
  if (FEEDBACK_PIN >= 0) pinMode(FEEDBACK_PIN, INPUT);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  config.token_status_callback = tokenStatusCallback;
  // Reuse a dedicated Firebase Auth account rather than creating anonymous users on every boot.
  auth.user.email = DEVICE_EMAIL;
  auth.user.password = DEVICE_PASSWORD;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

void loop() {
  bool ready = Firebase.ready(); // Keep token maintenance running.
  unsigned long now = millis();
  if (WiFi.status() != WL_CONNECTED) {
    if (now - lastReconnect >= 5000) { lastReconnect = now; WiFi.reconnect(); }
    delay(10);
    return;
  }
  if (ready) {
    if (now - lastPoll >= 500) {
      lastPoll = now;
      if (Firebase.RTDB.getString(&fbdo, "/eldroid/ledControl/command")) {
        String command = fbdo.stringData();
        if (command == "ON" || command == "OFF") {
          int target = command == "ON" ? HIGH : LOW;
          if (digitalRead(LED_PIN) != target) {
            digitalWrite(LED_PIN, target);
            delay(5);
            reportState();
          }
        }
      }
    }
    if (now - lastReport >= 2000) { lastReport = now; reportState(); }
  }
  delay(10);
}
