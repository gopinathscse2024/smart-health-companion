# Smart Health Companion AI
### Energy Harvesting Wearable Healthcare Device with Smart Desktop & Mobile Companion

A production-grade, modern Android application engineered with **Kotlin**, **Jetpack Compose (Material 3)**, **MVVM architecture**, **Room Database**, and **Bluetooth Low Energy (BLE)** integration for the ESP32-S3 Wearable Healthcare Monitor.

---

## 📱 Architecture & Technology Stack

- **UI & Presentation**: Modern Jetpack Compose with Material 3 design system, dark/light medical themes, fluid typography, and Canvas-based interactive charting.
- **Architecture**: MVVM with reactive Kotlin Coroutines, StateFlow, Repository, and Service layers.
- **Local Persistence**: Offline-first Room Database with indexing on timestamps, profiles, and measurement types.
- **BLE Integration**: Robust Android BLE Client connecting to ESP32-S3 with auto-reconnect, notification subscription, and streaming fragment buffer parser.
- **Predictive Analytics & Statistics**: On-device parametric & non-parametric statistical computing (Mean, Median, MAD, IQR, Linear Regression, EWMA, 95% Confidence Intervals).
- **Asthma & COPD Monitoring**: Respiratory symptom diary, peak expiratory flow (PEF) tracking, clinician action plan zones (Green/Yellow/Red), and emergency warning triage.
- **Reports & Export**: Native Android multi-page PDF generation (`PdfDocument`) and RFC 4180 CSV export with Android Share Sheet integration.
- **Privacy & Security**: 100% on-device processing by default; runtime BLE and GPS permission guards.

---

## 📡 ESP32-S3 Hardware Specification

- **Target Device Name**: `S3 Health Monitor`
- **Service UUID**: `6e400001-b5a3-f393-e0a9-e50e24dcca9e`
- **Sensor Data Characteristic (Notify)**: `6e400003-b5a3-f393-e0a9-e50e24dcca9e`
- **Command Characteristic (Write)**: `6e400004-b5a3-f393-e0a9-e50e24dcca9e`
- **CCCD UUID**: `00002902-0000-1000-8000-00805f9b34fb`

### 🎮 Mobile Application Bluetooth Commands

The Android application writes directly to the BLE Command Characteristic (`6e400004-...`) to control wearable operations:

| Command | Action | Operational Behavior |
| :--- | :--- | :--- |
| `START` | Turn on monitoring manually | Initiates manual DS18B20 temperature and MAX30102 heart rate scans, and immediately transmits barometric pressure, temperature, and BPM together. |
| `STOP` | Stop the current monitoring session | Shuts down the MAX30102 optical sensor and returns the ESP32-S3 to `IDLE` standby. |
| `AUTO_ON` | Enable automatic monitoring | Enables the periodic automatic monitoring schedule (runs auto temperature & heart scans every 60s). |
| `AUTO_OFF` | Disable automatic monitoring | Disables scheduled automatic cycles, halting background sensor wakeups. |
| `READ` | Request the latest available readings | Immediately reads and dispatches the latest valid DS18B20 temperature, MAX30102 BPM, BMP180 barometric pressure/altitude, and battery voltage. |
| `TIME:UNIX_SECONDS` | Set the clock using the phone's time | Parses the Unix epoch timestamp and synchronizes the ESP32-S3 internal clock via `settimeofday()`. |

> **Clock Synchronization Notice**: The Android companion app automatically sends `TIME:<current_unix_timestamp>` as soon as the BLE connection and GATT services are ready. Bluetooth connection alone does not automatically synchronize the clock.

### 📋 Firmware Pre-Upload Checklist

1. **Verify Arduino Libraries**: Confirm installation of `Adafruit SH110X`, `DallasTemperature`, `Adafruit BMP085`, `SparkFun MAX3010x`, and ESP32 BLE support.
2. **Battery Voltage Divider**: Confirm the 1:2 resistor divider (e.g., two 100kΩ resistors) is wired to GPIO 1 before setting `#define ENABLE_BATTERY_MEASUREMENT true`.
3. **Upload & Serial Monitor**: Flash the sketch to the ESP32-S3 and open the Serial Monitor at **115200 baud**.
4. **Validation Test**: Connect with a BLE testing app (or nRF Connect) first, write `START` to `6e400004-...`, and verify that temperature, heart rate, and pressure readings arrive together.
5. **Companion App Connection**: Connect via the Smart Health Companion Android app and use the interactive Remote Command buttons or quick-action bar.

### Supported Telemetry Formats

1. **Heart Rate (BPM)**:
   ```json
   {"type": "heart_rate", "value": 76.0, "unit": "BPM", "ms": 123456}
   ```
2. **Body Temperature (°C)**:
   ```json
   {"type": "temperature_c", "value": 36.5, "unit": "C", "ms": 123456}
   ```
3. **BMP180 Environmental Telemetry**:
   ```json
   {"type": "bmp180", "pressure_hpa": 1012.8, "temperature_c": 24.3, "altitude_m": 85.0, "ms": 123456}
   ```
   *Note: BMP180 sensor temperature is strictly tagged as internal hardware temperature and segregated from human body temperature. Atmospheric pressure is local station barometric pressure and is never treated as blood pressure.*

---

## 🗂️ Project File Structure

```
g:\healthcare app\
├── app\
│   ├── src\
│   │   ├── main\
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java\com\smarthealth\companion\
│   │   │   │   ├── SmartHealthApp.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── theme\
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Theme.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   ├── model\
│   │   │   │   │   ├── Measurement.kt
│   │   │   │   │   ├── MeasurementType.kt
│   │   │   │   │   ├── CommonEnums.kt
│   │   │   │   │   ├── RespiratorySymptomRecord.kt
│   │   │   │   │   └── AnalyticsModels.kt
│   │   │   │   ├── data\
│   │   │   │   │   ├── local\
│   │   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   │   ├── MeasurementDao.kt
│   │   │   │   │   │   ├── MeasurementEntity.kt
│   │   │   │   │   │   ├── SymptomRecordDao.kt
│   │   │   │   │   │   ├── SymptomRecordEntity.kt
│   │   │   │   │   │   └── PreferenceManager.kt
│   │   │   │   │   ├── ble\
│   │   │   │   │   │   ├── BleManager.kt
│   │   │   │   │   │   ├── BleJsonParser.kt
│   │   │   │   │   │   └── SampleDataGenerator.kt
│   │   │   │   │   └── location\
│   │   │   │   │       └── LocationProvider.kt
│   │   │   │   ├── analytics\
│   │   │   │   │   ├── StatisticalEngine.kt
│   │   │   │   │   ├── BaselineAnalyzer.kt
│   │   │   │   │   ├── PredictiveEngine.kt
│   │   │   │   │   └── AiSummaryGenerator.kt
│   │   │   │   ├── export\
│   │   │   │   │   ├── PdfReportGenerator.kt
│   │   │   │   │   ├── CsvExporter.kt
│   │   │   │   │   └── ShareHelper.kt
│   │   │   │   ├── repository\
│   │   │   │   │   └── HealthRepository.kt
│   │   │   │   └── ui\
│   │   │   │       ├── navigation\Screen.kt
│   │   │   │       ├── components\
│   │   │   │       │   ├── VitalMetricCard.kt
│   │   │   │       │   ├── InteractiveLineChart.kt
│   │   │   │       │   └── EmergencyAlertBanner.kt
│   │   │   │       ├── dashboard\DashboardScreen.kt
│   │   │   │       ├── ble\BleConnectionScreen.kt
│   │   │   │       ├── analytics\AnalysisCenterScreen.kt
│   │   │   │       ├── forecast\ForecastScreen.kt
│   │   │   │       ├── respiratory\RespiratoryScreen.kt
│   │   │   │       ├── sensor\SensorDetailsScreen.kt
│   │   │   │       ├── charts\VisualChartsScreen.kt
│   │   │   │       ├── explorer\TableExplorerScreen.kt
│   │   │   │       ├── reports\ReportsScreen.kt
│   │   │   │       ├── profile\ProfileScreen.kt
│   │   │   │       ├── privacy\PrivacySettingsScreen.kt
│   │   │   │       └── viewmodel\
│   │   │   │           ├── MainViewModel.kt
│   │   │   │           └── ViewModelFactory.kt
│   │   │   └── res\xml\file_paths.xml
│   │   └── test\java\com\smarthealth\companion\
│   │       ├── BleJsonParserTest.kt
│   │       ├── StatisticalEngineTest.kt
│   │       ├── PredictiveEngineTest.kt
│   │       └── BaselineAnalyzerTest.kt
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle\libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── local.properties
```

---

## 🛠️ Build and Installation Instructions

### Prerequisites
- JDK 17+
- Android SDK Platform 35 / Build-Tools 35.0.0
- Android Studio Ladybug / Koala or CLI

### Building the Project
Run from project root directory:
```bash
# Execute unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Acceptance Verification Matrix

| Requirement | Implementation Details | Test Coverage |
| :--- | :--- | :--- |
| **BLE Connection** | Scans and filters `S3 Health Monitor` with service `6e400001-...` | `BleManager.kt` & UI |
| **BLE Remote Commands** | Characteristic `6e400004-...` with `START`, `STOP`, `AUTO_ON`, `AUTO_OFF`, `READ`, `TIME:UNIX` | `BleManager.kt`, UI & firmware |
| **Clock Synchronization** | Auto-sends phone's Unix epoch upon connection via `TIME:UNIX_SECONDS` | `BleManager.kt` & `handleBleCommand` |
| **JSON Parser** | Supports streaming fragment concatenation, malformed/duplicate filtering | `BleJsonParserTest.kt` |
| **Room Offline DB** | Indexed entities (`phoneReceiptTimestampMs`, `type`, `profileId`) | `MeasurementDao.kt` |
| **1-30 Day Analysis** | Selectable 1d, 3d, 7d, 14d, 21d, 28d, 30d timeframes with Mean, Median, MAD, IQR, stdDev | `StatisticalEngineTest.kt` |
| **Safe Forecasting** | 6h, 24h, 3d, 7d horizons with 95% CI; fails gracefully when samples < 15 | `PredictiveEngineTest.kt` |
| **Asthma/COPD Tracker** | Symptom scores, PEF meter log, Action Plan zones, Red-flag emergency alert | `RespiratoryScreen.kt` |
| **Interactive Charts** | Custom Canvas charts with missing-data gap preservation and touch tooltips | `InteractiveLineChart.kt` |
| **Export Engine** | Multi-page clinical PDF report & RFC 4180 CSV export via Android Share Sheet | `PdfReportGenerator.kt` |
| **Simulation Mode** | High-fidelity synthetic stream with `DataSource.SIMULATED` tags | `SampleDataGenerator.kt` |
| **Privacy First** | 100% offline processing, location privacy toggle, and local data wipe | `PrivacySettingsScreen.kt` |
