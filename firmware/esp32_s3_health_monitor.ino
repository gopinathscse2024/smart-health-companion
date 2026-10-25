#include <Wire.h>
#include <WiFi.h>
#include <time.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SH110X.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <Adafruit_BMP085.h>
#include "MAX30105.h"
#include "heartRate.h"
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// ==================================================
// ESP32-S3 MINI HEALTH MONITOR
// OLED + DS18B20 + MAX30102 + TOUCH + WIFI + BLE
// ==================================================

// Pins
#define SDA_PIN 4
#define SCL_PIN 5
#define TOUCH_PIN 8
#define ONE_WIRE_BUS 13
#define TOUCH_ACTIVE_HIGH true

// Wi-Fi credentials
const char* WIFI_SSID = "12345678";
const char* WIFI_PASSWORD = "12345678";
const char* TZ_INFO = "IST-5:30";

// OLED: 128x64 SH1106
Adafruit_SH1106G display(128, 64, &Wire, -1);

// Temperature sensor
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature tempSensor(&oneWire);

// Heart-rate sensor
MAX30105 maxSensor;

// BMP180 barometric pressure sensor (shared I2C bus)
Adafruit_BMP085 bmp;
bool bmpReady = false;
float pressureHpa = 0;
float bmpTempC = 0;
float altitudeM = 0;
unsigned long lastBmpRead = 0;
const unsigned long BMP_REFRESH_MS = 2000;

// BLE service
#define SERVICE_UUID "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
#define DATA_UUID    "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
#define COMMAND_UUID "6e400004-b5a3-f393-e0a9-e50e24dcca9e"

BLECharacteristic* bleData = nullptr;
BLECharacteristic* bleCommand = nullptr;
volatile bool bleConnected = false;
bool autoMonitoringEnabled = true;

// Optional GPIO1 battery voltage measurement
// NOTE: Verify the battery voltage divider (e.g. 100k/100k) is wired before enabling!
#define BATTERY_PIN 1
#define ENABLE_BATTERY_MEASUREMENT false

// Forward declarations
void handleBleCommand(const String& cmd);
void startTemperatureMode(bool automatic);
void startHeartMode(bool automatic);
void stopSession();
void processBMP180();
void requestTemperature();
float readBatteryVoltage();
int getBatteryPercentage();

// ==================================================
// TIMING CONSTANTS -- ALL DEFINED
// ==================================================

const unsigned long HOLD_MS = 2500;
const unsigned long DEBOUNCE_MS = 40;
const unsigned long DOUBLE_TAP_MS = 350;
const unsigned long TEMP_REFRESH_MS = 1000;

const unsigned long AUTO_INTERVAL_MS = 60000UL;
const unsigned long AUTO_HEART_MS = 20000UL;
const unsigned long AUTO_TEMP_MS = 3000UL;
const unsigned long MANUAL_SESSION_MS = 300000UL;
const unsigned long TEMP_CONVERSION_MS = 750;
const unsigned long DISPLAY_MS = 100;
const unsigned long WIFI_RETRY_MS = 15000UL;

const byte TARGET_BPM_SAMPLES = 15;

// ==================================================
// SCREENS AND MODES
// ==================================================

enum Screen {
  HOME_SCREEN,
  MENU_SCREEN,
  TEMP_SCREEN,
  HEART_SCREEN,
  BMP_SCREEN,
  BATTERY_SCREEN
};

enum Mode {
  IDLE,
  AUTO_TEMP,
  AUTO_HEART,
  MANUAL_TEMP,
  MANUAL_HEART
};

Screen screenState = HOME_SCREEN;
Mode mode = IDLE;
int menuIndex = 0;

// ==================================================
// SENSOR DATA
// ==================================================

float tempC = 0;
float tempF = 0;
bool tempValid = false;
bool tempPending = false;
unsigned long tempReadyAt = 0;
unsigned long lastTempRequest = 0;

bool maxReady = false;
bool fingerPresent = false;
bool bpmValid = false;

long irValue = 0;
long lastBeat = 0;
unsigned long lastValidBeat = 0;
int bpm = 0;

const byte RATE_SIZE = 4;
byte rates[RATE_SIZE];
byte rateIndex = 0;
byte rateCount = 0;
byte bpmSamplesCollected = 0;

// ==================================================
// SYSTEM TIMERS
// ==================================================

unsigned long modeStarted = 0;
unsigned long modeDuration = 0;
unsigned long nextAuto = 0;
unsigned long lastDisplay = 0;
unsigned long lastWiFiAttempt = 0;
unsigned long lastIRPrint = 0;

// ==================================================
// TOUCH STATE
// ==================================================

bool rawTouchLast = false;
bool touchStable = false;
unsigned long touchChangeAt = 0;
unsigned long touchDownAt = 0;
bool holdHandled = false;

unsigned long lastReleaseAt = 0;
bool waitingSecondTap = false;

// ==================================================
// BLE CALLBACKS
// ==================================================

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* server) override {
    bleConnected = true;
    Serial.println("BLE connected");
  }

  void onDisconnect(BLEServer* server) override {
    bleConnected = false;
    Serial.println("BLE disconnected");
    BLEDevice::startAdvertising();
  }
};

class CommandCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* characteristic) override {
    std::string rxValue = characteristic->getValue();
    if (rxValue.length() > 0) {
      String cmd = String(rxValue.c_str());
      cmd.trim();
      handleBleCommand(cmd);
    }
  }
};

// ==================================================
// SEND DATA TO SERIAL AND BLE
// ==================================================

void sendData(const char* type, float value, const char* unit) {
  char payload[150];

  snprintf(
    payload, sizeof(payload),
    "{\"type\":\"%s\",\"value\":%.2f,\"unit\":\"%s\",\"ms\":%lu}",
    type, value, unit, millis()
  );

  Serial.println(payload);

  if (bleConnected && bleData != nullptr) {
    bleData->setValue(
      (uint8_t*)payload,
      strlen(payload)
    );
    bleData->notify();
  }
}

// ==================================================
// INITIALIZE BLUETOOTH
// ==================================================

void setupBLE() {
  BLEDevice::init("S3 Health Monitor");

  BLEServer* server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEService* service = server->createService(SERVICE_UUID);

  // Sensor data stream notification characteristic
  bleData = service->createCharacteristic(
    DATA_UUID,
    BLECharacteristic::PROPERTY_READ |
    BLECharacteristic::PROPERTY_NOTIFY
  );
  bleData->addDescriptor(new BLE2902());
  bleData->setValue("Health monitor ready");

  // Mobile application remote command characteristic
  bleCommand = service->createCharacteristic(
    COMMAND_UUID,
    BLECharacteristic::PROPERTY_WRITE |
    BLECharacteristic::PROPERTY_WRITE_NR
  );
  bleCommand->setCallbacks(new CommandCallbacks());

  service->start();
  BLEDevice::startAdvertising();

  Serial.println("BLE advertising started (Data & Command characteristics ready)");
}

// ==================================================
// WIFI AND CLOCK
// ==================================================

void maintainWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;

  if (millis() - lastWiFiAttempt >= WIFI_RETRY_MS) {
    lastWiFiAttempt = millis();
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  }
}

bool timeValid() {
  return time(nullptr) > 1672531200; // > Jan 1, 2023 - valid after phone or NTP sync
}

// ==================================================
// DISPLAY ICONS
// ==================================================

void drawHeart(int x, int y, int size, bool filled) {
  int r = size / 4;

  if (filled) {
    display.fillCircle(x - r, y - r / 2, r, SH110X_WHITE);
    display.fillCircle(x + r, y - r / 2, r, SH110X_WHITE);
    display.fillTriangle(
      x - size / 2, y,
      x + size / 2, y,
      x, y + size / 2,
      SH110X_WHITE
    );
  } else {
    display.drawCircle(x - r, y - r / 2, r, SH110X_WHITE);
    display.drawCircle(x + r, y - r / 2, r, SH110X_WHITE);
    display.drawLine(x - size / 2, y, x, y + size / 2, SH110X_WHITE);
    display.drawLine(x + size / 2, y, x, y + size / 2, SH110X_WHITE);
    display.drawLine(x - size / 2, y, x - r, y - r / 2, SH110X_WHITE);
    display.drawLine(x + size / 2, y, x + r, y - r / 2, SH110X_WHITE);
  }
}

void drawThermometer(int x, int y) {
  display.drawRoundRect(x - 2, y - 7, 5, 13, 2, SH110X_WHITE);
  display.fillCircle(x, y + 6, 4, SH110X_WHITE);
  display.drawLine(x, y - 3, x, y + 5, SH110X_WHITE);
}

void drawWiFiIcon(int x, int y) {
  if (WiFi.status() == WL_CONNECTED) {
    display.drawLine(x, y + 3, x + 4, y, SH110X_WHITE);
    display.drawLine(x + 4, y, x + 8, y + 3, SH110X_WHITE);
    display.drawLine(x + 2, y + 5, x + 4, y + 3, SH110X_WHITE);
    display.drawLine(x + 4, y + 3, x + 6, y + 5, SH110X_WHITE);
    display.drawPixel(x + 4, y + 7, SH110X_WHITE);
  } else {
    display.drawLine(x, y, x + 7, y + 7, SH110X_WHITE);
    display.drawLine(x + 7, y, x, y + 7, SH110X_WHITE);
  }
}

void drawBluetoothIcon(int x, int y) {
  if (bleConnected) {
    display.drawLine(x + 3, y, x + 3, y + 8, SH110X_WHITE);
    display.drawLine(x + 3, y, x + 7, y + 3, SH110X_WHITE);
    display.drawLine(x + 7, y + 3, x + 3, y + 5, SH110X_WHITE);
    display.drawLine(x + 3, y + 5, x + 7, y + 8, SH110X_WHITE);
    display.drawLine(x + 7, y + 8, x + 3, y + 11, SH110X_WHITE);
    display.drawLine(x + 3, y + 5, x, y + 2, SH110X_WHITE);
    display.drawLine(x + 3, y + 5, x, y + 8, SH110X_WHITE);
  } else {
    display.drawRect(x, y, 7, 7, SH110X_WHITE);
  }
}

void drawStatus() {
  display.setTextSize(1);
  display.setTextColor(SH110X_WHITE);

  drawWiFiIcon(2, 1);
  drawBluetoothIcon(15, 0);

  display.setCursor(30, 2);
  display.print("HEALTH");

  display.drawFastHLine(0, 13, 128, SH110X_WHITE);
}

// ==================================================
// HOME SCREEN
// ==================================================

void drawHome() {
  display.clearDisplay();
  drawStatus();

  struct tm ti;
  bool valid = timeValid() && getLocalTime(&ti, 0);

  display.setTextSize(2);

  if (valid) {
    int h = ti.tm_hour % 12;
    if (h == 0) h = 12;

    char clockText[12];
    snprintf(clockText, sizeof(clockText), "%02d:%02d", h, ti.tm_min);

    display.setCursor(23, 17);
    display.print(clockText);

    display.setTextSize(1);
    display.setCursor(83, 35);
    display.print(ti.tm_hour >= 12 ? "PM" : "AM");

    display.setCursor(0, 37);
    display.printf(
      "%02d/%02d/%04d",
      ti.tm_mday, ti.tm_mon + 1, ti.tm_year + 1900
    );
  } else {
    unsigned long mins = millis() / 60000UL;
    int h = (mins / 60) % 12;
    if (h == 0) h = 12;

    char clockText[12];
    snprintf(clockText, sizeof(clockText), "%02d:%02lu", h, mins % 60);

    display.setCursor(23, 20);
    display.print(clockText);

    display.setTextSize(1);
    display.setCursor(83, 35);
    display.print("AM");
    display.setCursor(0, 37);
    display.print("Clock needs Wi-Fi");
  }

  display.drawRoundRect(0, 47, 61, 16, 3, SH110X_WHITE);
  display.drawRoundRect(66, 47, 62, 16, 3, SH110X_WHITE);

  bool heartActive = mode == AUTO_HEART || mode == MANUAL_HEART;
  bool pump = heartActive && ((millis() / 250) % 2 == 0);

  drawHeart(76, 54, pump ? 11 : 8, pump);

  display.setTextSize(1);
  display.setCursor(87, 52);

  if (bpmValid && bpm > 0) display.print(bpm);
  else display.print("--");

  display.setCursor(5, 52);

  if (tempValid) display.printf("%.1fC", tempC);
  else display.print("--.-C");

  display.display();
}

// ==================================================
// MENU SCREEN
// ==================================================

void drawMenu() {
  display.clearDisplay();
  drawStatus();
  display.setTextSize(1);
  display.setCursor(4, 17);
  display.print("SELECT SENSOR");

  const char* labels[] = {"Temperature", "Heart Rate", "BMP180 Pressure", "Battery", "Home"};
  for (int i = 0; i < 5; i++) {
    int y = 27 + i * 7;
    if (i == menuIndex) {
      display.fillRoundRect(2, y - 1, 124, 8, 2, SH110X_WHITE);
      display.setTextColor(SH110X_BLACK);
    } else {
      display.setTextColor(SH110X_WHITE);
    }
    display.setCursor(7, y);
    display.print(i == menuIndex ? "> " : "  ");
    display.print(labels[i]);
  }
  display.setTextColor(SH110X_WHITE);
  display.display();
}

// ==================================================
// SENSOR SCREENS
// ==================================================

void drawTemperatureScreen() {
  display.clearDisplay();
  drawStatus();

  display.setCursor(8, 18);
  display.print("BODY TEMPERATURE");

  if (tempValid) {
    display.setTextSize(2);
    display.setCursor(17, 30);
    display.printf("%.1f C", tempC);

    display.setTextSize(1);
    display.setCursor(32, 50);
    display.printf("%.1f F", tempF);
  } else {
    display.setCursor(15, 37);
    display.print("Reading sensor...");
  }

  display.display();
}

void drawHeartScreen() {
  display.clearDisplay();
  drawStatus();

  display.setCursor(22, 18);
  display.print("HEART RATE");

  bool active = mode == AUTO_HEART || mode == MANUAL_HEART;
  bool pump = active && ((millis() / 250) % 2 == 0);

  drawHeart(22, 39, pump ? 18 : 14, pump);

  display.setTextSize(2);
  display.setCursor(43, 31);

  if (bpmValid && bpm > 0) display.printf("%d", bpm);
  else display.print("--");

  display.setTextSize(1);
  display.setCursor(43, 49);
  display.print(fingerPresent ? "BPM" : "Place finger");

  display.display();
}

void drawBmpScreen() {
  display.clearDisplay();
  drawStatus();
  display.setTextSize(1);
  display.setCursor(8, 17);
  display.print("BMP180 ENVIRONMENT");
  if (bmpReady) {
    display.setCursor(5, 29);
    display.printf("Pressure: %.1f hPa", pressureHpa);
    display.setCursor(5, 40);
    display.printf("Air temp: %.1f C", bmpTempC);
    display.setCursor(5, 51);
    display.printf("Altitude: %.1f m", altitudeM);
  } else {
    display.setCursor(10, 38);
    display.print("BMP180 not detected");
  }
  display.display();
}

// ==================================================
// BATTERY MEASUREMENT (GPIO 1)
// ==================================================

float readBatteryVoltage() {
#if ENABLE_BATTERY_MEASUREMENT
  // 12-bit ADC on ESP32-S3: 0..4095 representing 0..3.3V
  // With 1:2 voltage divider (e.g. two 100k resistors), max measurable is ~6.6V
  int raw = analogRead(BATTERY_PIN);
  float voltage = (raw / 4095.0f) * 3.3f * 2.0f;
  return voltage;
#else
  return -1.0f;
#endif
}

int getBatteryPercentage() {
#if ENABLE_BATTERY_MEASUREMENT
  float v = readBatteryVoltage();
  if (v <= 0.0f) return -1;
  // LiPo 1S curve approx: 3.30V = 0%, 4.20V = 100%
  int pct = (int)((v - 3.30f) / (4.20f - 3.30f) * 100.0f);
  if (pct < 0) pct = 0;
  if (pct > 100) pct = 100;
  return pct;
#else
  return -1;
#endif
}

void drawBatteryScreen() {
  display.clearDisplay();
  drawStatus();

  display.setCursor(18, 18);
  display.print("BATTERY STATUS");

#if ENABLE_BATTERY_MEASUREMENT
  float v = readBatteryVoltage();
  int pct = getBatteryPercentage();

  display.setTextSize(2);
  display.setCursor(35, 30);
  display.printf("%d%%", pct);

  display.setTextSize(1);
  display.setCursor(20, 50);
  display.printf("%.2f V (GPIO 1)", v);
#else
  display.setTextSize(1);
  display.setCursor(10, 32);
  display.print("GPIO 1 Divider");
  display.setCursor(10, 44);
  display.print("Confirm wiring first");
  display.setCursor(10, 54);
  display.print("Enable in sketch");
#endif

  display.display();
}

void refreshDisplay() {
  switch (screenState) {
    case HOME_SCREEN: drawHome(); break;
    case MENU_SCREEN: drawMenu(); break;
    case TEMP_SCREEN: drawTemperatureScreen(); break;
    case HEART_SCREEN: drawHeartScreen(); break;
    case BMP_SCREEN: drawBmpScreen(); break;
    case BATTERY_SCREEN: drawBatteryScreen(); break;
  }
}

// ==================================================
// TEMPERATURE SENSOR
// ==================================================

void requestTemperature() {
  tempSensor.requestTemperatures();
  tempReadyAt = millis() + TEMP_CONVERSION_MS;
  tempPending = true;
}

void processTemperature() {
  if (!tempPending || millis() < tempReadyAt) return;

  tempPending = false;

  float t = tempSensor.getTempCByIndex(0);

  if (t != DEVICE_DISCONNECTED_C && t > -100 && t < 125) {
    tempC = t;
    tempF = t * 9.0 / 5.0 + 32.0;
    tempValid = true;

    Serial.printf("Temperature: %.2f C / %.2f F\n", tempC, tempF);
    sendData("temperature_c", tempC, "C");
  } else {
    tempValid = false;
    Serial.println("DS18B20 error: check GPIO13 and 4.7k pull-up");
  }
}

// ==================================================
// BMP180 SENSOR
// ==================================================

void processBMP180() {
  if (!bmpReady || millis() - lastBmpRead < BMP_REFRESH_MS) return;
  lastBmpRead = millis();
  pressureHpa = bmp.readPressure() / 100.0f;
  bmpTempC = bmp.readTemperature();
  altitudeM = bmp.readAltitude(101325.0f);
  if (pressureHpa > 0 && pressureHpa < 1200) {
    sendData("pressure_hpa", pressureHpa, "hPa");
    sendData("bmp180_temperature_c", bmpTempC, "C");
    sendData("altitude_m", altitudeM, "m");
  }
}

// ==================================================
// MAX30102 HEART RATE SENSOR
// ==================================================

bool startMAX() {
  if (!maxReady) {
    if (!maxSensor.begin(Wire, I2C_SPEED_STANDARD)) {
      Serial.println("MAX30102 initialization failed!");
      return false;
    }

    maxReady = true;
  }

  // Moderate LED brightness; increase only after hardware is verified safe.
  maxSensor.setup(0x1F, 4, 2, 100, 411, 4096);
  maxSensor.clearFIFO();

  fingerPresent = false;
  bpmValid = false;
  bpm = 0;
  rateIndex = 0;
  rateCount = 0;
  lastBeat = 0;
  lastValidBeat = 0;
  bpmSamplesCollected = 0;

  Serial.println("MAX30102 active");
  return true;
}

void stopMAX() {
  if (maxReady) {
    maxSensor.shutDown();
  }

  fingerPresent = false;
  bpmValid = false;

  Serial.println("MAX30102 standby");
}

void readHeart() {
  if (mode != AUTO_HEART && mode != MANUAL_HEART) return;

  irValue = maxSensor.getIR();
  fingerPresent = irValue > 10000;

  if (millis() - lastIRPrint >= 1000) {
    lastIRPrint = millis();
    Serial.printf("IR value: %ld\n", irValue);
  }

  if (!fingerPresent) {
    if (millis() - lastValidBeat > 5000) {
      bpmValid = false;
    }

    lastBeat = 0;
    return;
  }

  if (checkForBeat(irValue)) {
    unsigned long now = millis();

    if (lastBeat != 0) {
      unsigned long delta = now - lastBeat;

      if (delta > 0) {
        float value = 60000.0 / delta;

        if (value >= 40 && value <= 200) {
          rates[rateIndex] = (byte)(value + 0.5);
          rateIndex = (rateIndex + 1) % RATE_SIZE;

          if (rateCount < RATE_SIZE) rateCount++;

          int sum = 0;

          for (byte i = 0; i < rateCount; i++) {
            sum += rates[i];
          }

          bpm = sum / rateCount;
          bpmValid = rateCount >= 2;
          lastValidBeat = now;

          if (mode == AUTO_HEART &&
              bpmSamplesCollected < TARGET_BPM_SAMPLES) {
            bpmSamplesCollected++;
          }

          Serial.printf("Heart rate: %d BPM\n", bpm);

          if (bpmValid) {
            sendData("heart_rate", bpm, "BPM");
          }
        }
      }
    }

    lastBeat = now;
  }

  if (millis() - lastValidBeat > 5000) {
    bpmValid = false;
  }
}

// ==================================================
// SENSOR SESSION CONTROL
// ==================================================

void startTemperatureMode(bool automatic) {
  mode = automatic ? AUTO_TEMP : MANUAL_TEMP;
  modeStarted = millis();
  modeDuration = automatic ? AUTO_TEMP_MS : MANUAL_SESSION_MS;

  lastTempRequest = millis();
  requestTemperature();

  Serial.println(automatic
    ? "Automatic temperature scan started"
    : "Manual temperature scan started");
}

void startHeartMode(bool automatic) {
  if (!startMAX()) {
    mode = IDLE;
    return;
  }

  mode = automatic ? AUTO_HEART : MANUAL_HEART;
  modeStarted = millis();
  modeDuration = automatic ? AUTO_HEART_MS : MANUAL_SESSION_MS;

  Serial.println(automatic
    ? "Automatic heart scan started"
    : "Manual heart scan started");
}

void stopSession() {
  if (mode == AUTO_HEART || mode == MANUAL_HEART) {
    stopMAX();
  }

  mode = IDLE;
  Serial.println("Monitoring session stopped");
}

void enterSelectedOption() {
  if (menuIndex == 0) {
    screenState = TEMP_SCREEN;
    startTemperatureMode(false);
  } else if (menuIndex == 1) {
    screenState = HEART_SCREEN;
    startHeartMode(false);
  } else if (menuIndex == 2) {
    screenState = BMP_SCREEN;
    mode = IDLE;
  } else if (menuIndex == 3) {
    screenState = BATTERY_SCREEN;
    mode = IDLE;
  } else {
    stopSession();
    screenState = HOME_SCREEN;
  }
}

// ==================================================
// BLE REMOTE COMMAND HANDLER
// ==================================================

void handleBleCommand(const String& cmd) {
  if (cmd.equalsIgnoreCase("START")) {
    Serial.println("BLE Command: START -> Turning on monitoring manually");
    screenState = HOME_SCREEN;
    startTemperatureMode(false);
    startHeartMode(false);

    // Transmit latest sensor telemetry together immediately
    if (bmpReady) {
      pressureHpa = bmp.readPressure() / 100.0f;
      bmpTempC = bmp.readTemperature();
      altitudeM = bmp.readAltitude(101325.0f);
      sendData("pressure_hpa", pressureHpa, "hPa");
      sendData("bmp180_temperature_c", bmpTempC, "C");
      sendData("altitude_m", altitudeM, "m");
    }
    if (tempValid) {
      sendData("temperature_c", tempC, "C");
    }
    if (bpmValid && bpm > 0) {
      sendData("heart_rate", bpm, "BPM");
    }
  } else if (cmd.equalsIgnoreCase("STOP")) {
    Serial.println("BLE Command: STOP -> Stopping current monitoring session");
    stopSession();
    screenState = HOME_SCREEN;
  } else if (cmd.equalsIgnoreCase("AUTO_ON")) {
    Serial.println("BLE Command: AUTO_ON -> Automatic monitoring enabled");
    autoMonitoringEnabled = true;
    nextAuto = millis();
  } else if (cmd.equalsIgnoreCase("AUTO_OFF")) {
    Serial.println("BLE Command: AUTO_OFF -> Automatic monitoring disabled");
    autoMonitoringEnabled = false;
    if (mode == AUTO_TEMP || mode == AUTO_HEART) {
      stopSession();
    }
  } else if (cmd.equalsIgnoreCase("READ")) {
    Serial.println("BLE Command: READ -> Requesting latest available readings");
    if (tempValid) {
      sendData("temperature_c", tempC, "C");
    } else {
      requestTemperature();
    }
    if (bpmValid && bpm > 0) {
      sendData("heart_rate", bpm, "BPM");
    }
    if (bmpReady) {
      pressureHpa = bmp.readPressure() / 100.0f;
      bmpTempC = bmp.readTemperature();
      altitudeM = bmp.readAltitude(101325.0f);
      sendData("pressure_hpa", pressureHpa, "hPa");
      sendData("bmp180_temperature_c", bmpTempC, "C");
      sendData("altitude_m", altitudeM, "m");
    }
#if ENABLE_BATTERY_MEASUREMENT
    int pct = getBatteryPercentage();
    if (pct >= 0) {
      sendData("battery", (float)pct, "%");
    }
#endif
  } else if (cmd.startsWith("TIME:")) {
    String secStr = cmd.substring(5);
    secStr.trim();
    unsigned long long unixSec = strtoull(secStr.c_str(), NULL, 10);
    if (unixSec > 1000000000ULL) {
      struct timeval tv;
      tv.tv_sec = (time_t)unixSec;
      tv.tv_usec = 0;
      settimeofday(&tv, NULL);
      Serial.printf("BLE Command: TIME -> Clock synchronized to Unix %llu\n", unixSec);
    } else {
      Serial.printf("BLE Command: TIME -> Invalid timestamp string '%s'\n", secStr.c_str());
    }
  } else {
    Serial.printf("BLE Command: Unknown '%s'\n", cmd.c_str());
  }
}

// ==================================================
// TOUCH CONTROL
// ==================================================

bool touchRead() {
  int value = digitalRead(TOUCH_PIN);

#if TOUCH_ACTIVE_HIGH
  return value == HIGH;
#else
  return value == LOW;
#endif
}

void singleTap() {
  if (screenState == MENU_SCREEN) {
    menuIndex = (menuIndex + 1) % 5;
  } else if (screenState == TEMP_SCREEN ||
             screenState == HEART_SCREEN ||
             screenState == BMP_SCREEN ||
             screenState == BATTERY_SCREEN) {
    screenState = HOME_SCREEN;
  }
}

void doubleTap() {
  if (screenState == HOME_SCREEN) {
    screenState = MENU_SCREEN;
  }
}

void longHold() {
  if (screenState == MENU_SCREEN) {
    enterSelectedOption();
  } else if (screenState == TEMP_SCREEN ||
             screenState == HEART_SCREEN ||
             screenState == BMP_SCREEN) {
    stopSession();
    screenState = HOME_SCREEN;
  } else if (screenState == BATTERY_SCREEN) {
    screenState = HOME_SCREEN;
  }
}

void processTouch() {
  unsigned long now = millis();
  bool raw = touchRead();

  if (raw != rawTouchLast) {
    rawTouchLast = raw;
    touchChangeAt = now;
  }

  if (now - touchChangeAt < DEBOUNCE_MS) return;

  if (raw != touchStable) {
    touchStable = raw;

    if (touchStable) {
      touchDownAt = now;
      holdHandled = false;
    } else if (!holdHandled) {
      if (waitingSecondTap &&
          now - lastReleaseAt <= DOUBLE_TAP_MS) {
        waitingSecondTap = false;
        doubleTap();
      } else {
        waitingSecondTap = true;
        lastReleaseAt = now;
      }
    }
  }

  if (touchStable && !holdHandled &&
      now - touchDownAt >= HOLD_MS) {
    holdHandled = true;
    waitingSecondTap = false;
    longHold();
  }

  if (waitingSecondTap &&
      now - lastReleaseAt > DOUBLE_TAP_MS) {
    waitingSecondTap = false;
    singleTap();
  }
}

// ==================================================
// AUTOMATIC SCHEDULING
// ==================================================

void startAutomaticCycle() {
  if (mode != IDLE) return;

  startTemperatureMode(true);
}

void processSchedule() {
  unsigned long now = millis();

  if (autoMonitoringEnabled && mode == IDLE && (long)(now - nextAuto) >= 0) {
    nextAuto = now + AUTO_INTERVAL_MS;
    startAutomaticCycle();
  }

  if (mode == AUTO_TEMP &&
      now - modeStarted >= modeDuration) {
    startHeartMode(true);
  }

  if (mode == AUTO_HEART) {
    if (bpmSamplesCollected >= TARGET_BPM_SAMPLES ||
        now - modeStarted >= modeDuration) {
      stopMAX();
      mode = IDLE;

      Serial.printf(
        "Automatic BPM samples collected: %u\n",
        bpmSamplesCollected
      );
    }
  }

  if ((mode == MANUAL_TEMP || mode == MANUAL_HEART) &&
      now - modeStarted >= MANUAL_SESSION_MS) {
    stopSession();
  }
}

// ==================================================
// SETUP
// ==================================================

void setup() {
  Serial.begin(115200);
  delay(500);

#if TOUCH_ACTIVE_HIGH
  pinMode(TOUCH_PIN, INPUT);
#else
  pinMode(TOUCH_PIN, INPUT_PULLUP);
#endif

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  if (!display.begin(0x3C, true)) {
    Serial.println("SH1106 initialization failed!");
    while (true) delay(1000);
  }

  display.clearDisplay();
  display.setTextColor(SH110X_WHITE);
  display.setTextSize(1);
  display.setCursor(15, 22);
  display.println("HEALTH MONITOR");
  display.setCursor(25, 38);
  display.println("Starting...");
  display.display();

  tempSensor.begin();
  tempSensor.setWaitForConversion(false);

  bmpReady = bmp.begin();
  Serial.println(bmpReady ? "BMP180 detected" : "BMP180 not detected");

  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  lastWiFiAttempt = millis();

  configTzTime(TZ_INFO, "pool.ntp.org", "time.google.com");

  setupBLE();

  maxReady = false;
  mode = IDLE;

  // First automatic cycle starts immediately after boot.
  nextAuto = millis();

  Serial.println("System ready");
  Serial.println("Home: double tap to open menu");
  Serial.println("Menu: single tap to move through 5 options");
  Serial.println("Menu: hold 2.5 seconds to enter");
  Serial.println("Sensor screen: hold 2.5 seconds to exit");
}

// ==================================================
// MAIN LOOP
// ==================================================

void loop() {
  maintainWiFi();
  processTouch();

  if ((mode == MANUAL_TEMP || mode == AUTO_TEMP) &&
      !tempPending &&
      millis() - lastTempRequest >= TEMP_REFRESH_MS) {
    lastTempRequest = millis();
    requestTemperature();
  }

  processTemperature();
  processBMP180();

  if (mode == MANUAL_HEART || mode == AUTO_HEART) {
    readHeart();
  }

  processSchedule();

  if (millis() - lastDisplay >= DISPLAY_MS) {
    lastDisplay = millis();
    refreshDisplay();
  }

  delay(2);
}
