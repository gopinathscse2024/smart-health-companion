package com.smarthealth.companion.data.ble

import com.smarthealth.companion.model.ActionPlanZone
import com.smarthealth.companion.model.BaselineComparison
import com.smarthealth.companion.model.DataQualityStatus
import com.smarthealth.companion.model.DataSource
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.model.RespiratorySymptomRecord
import java.util.UUID
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-fidelity demonstration and sample data generator.
 * Strictly labels every generated record as DataSource.SIMULATED.
 * Used for testing offline analytics, forecasting, charts, and export when hardware is disconnected.
 */
class SampleDataGenerator {

    private val random = Random(42)

    /**
     * Generates a single real-time simulated telemetry packet.
     */
    fun generateLivePacket(): List<Measurement> {
        val now = System.currentTimeMillis()
        val baseTimeSec = (now / 1000.0)

        // Circadian-inspired baseline with realistic natural variation
        val hr = 72.0 + 6.0 * sin(baseTimeSec / 20.0) + random.nextDouble(-2.0, 2.0)
        val bodyTemp = 36.6 + 0.2 * sin(baseTimeSec / 60.0) + random.nextDouble(-0.05, 0.05)
        val pressure = 1012.5 + 0.8 * sin(baseTimeSec / 120.0) + random.nextDouble(-0.1, 0.1)
        val sensorTemp = 24.8 + random.nextDouble(-0.2, 0.2) // BMP180 sensor temp
        val altitude = 44330.0 * (1.0 - Math.pow(pressure / 1013.25, 0.19029495718363465))
        val spo2 = 98.0 + random.nextInt(-1, 2)

        return listOf(
            Measurement(
                id = UUID.randomUUID().toString(),
                type = MeasurementType.HEART_RATE,
                numericValue = hr,
                unit = "BPM",
                deviceTimestampMs = now - 50,
                phoneReceiptTimestampMs = now,
                source = DataSource.SIMULATED,
                deviceId = "ESP32-S3-SIMULATOR",
                dataQualityStatus = DataQualityStatus.GOOD,
                baselineComparison = BaselineComparison.WITHIN_BASELINE
            ),
            Measurement(
                id = UUID.randomUUID().toString(),
                type = MeasurementType.BODY_TEMPERATURE,
                numericValue = bodyTemp,
                unit = "°C",
                deviceTimestampMs = now - 50,
                phoneReceiptTimestampMs = now,
                source = DataSource.SIMULATED,
                deviceId = "ESP32-S3-SIMULATOR",
                dataQualityStatus = DataQualityStatus.GOOD,
                baselineComparison = BaselineComparison.WITHIN_BASELINE
            ),
            Measurement(
                id = UUID.randomUUID().toString(),
                type = MeasurementType.ATMOSPHERIC_PRESSURE,
                numericValue = pressure,
                unit = "hPa",
                deviceTimestampMs = now - 50,
                phoneReceiptTimestampMs = now,
                source = DataSource.SIMULATED,
                deviceId = "ESP32-S3-SIMULATOR",
                dataQualityStatus = DataQualityStatus.GOOD,
                baselineComparison = BaselineComparison.WITHIN_BASELINE
            ),
            Measurement(
                id = UUID.randomUUID().toString(),
                type = MeasurementType.SENSOR_TEMPERATURE,
                numericValue = sensorTemp,
                unit = "°C",
                deviceTimestampMs = now - 50,
                phoneReceiptTimestampMs = now,
                source = DataSource.SIMULATED,
                deviceId = "ESP32-S3-SIMULATOR",
                dataQualityStatus = DataQualityStatus.GOOD,
                baselineComparison = BaselineComparison.WITHIN_BASELINE
            ),
            Measurement(
                id = UUID.randomUUID().toString(),
                type = MeasurementType.ESTIMATED_ALTITUDE,
                numericValue = altitude,
                unit = "m",
                deviceTimestampMs = now - 50,
                phoneReceiptTimestampMs = now,
                source = DataSource.SIMULATED,
                deviceId = "ESP32-S3-SIMULATOR",
                dataQualityStatus = DataQualityStatus.GOOD,
                baselineComparison = BaselineComparison.WITHIN_BASELINE
            ),
            Measurement(
                id = UUID.randomUUID().toString(),
                type = MeasurementType.SPO2,
                numericValue = spo2.toDouble(),
                unit = "%",
                deviceTimestampMs = now - 50,
                phoneReceiptTimestampMs = now,
                source = DataSource.SIMULATED,
                deviceId = "ESP32-S3-SIMULATOR",
                dataQualityStatus = DataQualityStatus.GOOD,
                baselineComparison = BaselineComparison.WITHIN_BASELINE
            )
        )
    }

    /**
     * Generates 30 days of realistic historical data across multiple vital and environmental signals.
     * Incorporates diurnal rhythms, slight exercise spikes, and clear anomaly events.
     */
    fun generate30DayHistoricalData(): List<Measurement> {
        val list = mutableListOf<Measurement>()
        val now = System.currentTimeMillis()
        val oneHourMs = 3600_000L
        val totalHours = 30 * 24

        for (h in totalHours downTo 1) {
            val timestamp = now - (h * oneHourMs)
            val hourOfDay = ((timestamp / oneHourMs) % 24).toInt()

            // Diurnal HR curve: lower during sleep (hours 0-6), higher midday
            val isSleeping = hourOfDay in 0..6
            val isExercise = hourOfDay == 18 && (h % 3 == 0) // Every 3 days evening workout

            val hr = when {
                isExercise -> random.nextDouble(125.0, 155.0)
                isSleeping -> random.nextDouble(54.0, 64.0)
                else -> random.nextDouble(68.0, 84.0)
            }

            val bodyTemp = if (isSleeping) {
                random.nextDouble(36.2, 36.6)
            } else {
                random.nextDouble(36.5, 37.1)
            }

            // Weather front simulated pressure shifts
            val weatherCycle = sin(h / 48.0) * 12.0
            val pressure = 1013.25 + weatherCycle + random.nextDouble(-1.5, 1.5)
            val sensorTemp = 22.0 + 3.0 * sin(hourOfDay / 24.0 * Math.PI) + random.nextDouble(-0.5, 0.5)
            val altitude = 44330.0 * (1.0 - Math.pow(pressure / 1013.25, 0.19029495718363465))

            // Anomaly check: flag high HR outside exercise as unusual change
            val hrBaselineComparison = if (hr > 110.0 && !isExercise) {
                BaselineComparison.UNUSUAL_CHANGE
            } else {
                BaselineComparison.WITHIN_BASELINE
            }

            list.add(
                Measurement(
                    type = MeasurementType.HEART_RATE,
                    numericValue = hr,
                    unit = "BPM",
                    deviceTimestampMs = timestamp,
                    phoneReceiptTimestampMs = timestamp,
                    source = DataSource.SIMULATED,
                    deviceId = "ESP32-S3-SIMULATOR",
                    baselineComparison = hrBaselineComparison
                )
            )

            list.add(
                Measurement(
                    type = MeasurementType.BODY_TEMPERATURE,
                    numericValue = bodyTemp,
                    unit = "°C",
                    deviceTimestampMs = timestamp,
                    phoneReceiptTimestampMs = timestamp,
                    source = DataSource.SIMULATED,
                    deviceId = "ESP32-S3-SIMULATOR"
                )
            )

            list.add(
                Measurement(
                    type = MeasurementType.ATMOSPHERIC_PRESSURE,
                    numericValue = pressure,
                    unit = "hPa",
                    deviceTimestampMs = timestamp,
                    phoneReceiptTimestampMs = timestamp,
                    source = DataSource.SIMULATED,
                    deviceId = "ESP32-S3-SIMULATOR"
                )
            )

            list.add(
                Measurement(
                    type = MeasurementType.SENSOR_TEMPERATURE,
                    numericValue = sensorTemp,
                    unit = "°C",
                    deviceTimestampMs = timestamp,
                    phoneReceiptTimestampMs = timestamp,
                    source = DataSource.SIMULATED,
                    deviceId = "ESP32-S3-SIMULATOR"
                )
            )

            list.add(
                Measurement(
                    type = MeasurementType.ESTIMATED_ALTITUDE,
                    numericValue = altitude,
                    unit = "m",
                    deviceTimestampMs = timestamp,
                    phoneReceiptTimestampMs = timestamp,
                    source = DataSource.SIMULATED,
                    deviceId = "ESP32-S3-SIMULATOR"
                )
            )
        }

        return list
    }

    /**
     * Generates sample respiratory logs for Asthma and COPD monitoring.
     */
    fun generateSampleRespiratoryLogs(): List<RespiratorySymptomRecord> {
        val now = System.currentTimeMillis()
        val oneDay = 86400_000L

        return listOf(
            RespiratorySymptomRecord(
                timestampMs = now - (oneDay * 0) - 7200_000,
                breathlessnessScore = 1,
                wheezing = false,
                cough = false,
                chestTightness = false,
                activityLevel = "Light Walking",
                sleepDisrupted = false,
                environmentalTriggers = "None",
                rescueMedicationPuffs = 0,
                peakExpiratoryFlowLpm = 490.0,
                actionPlanZone = ActionPlanZone.GREEN,
                notes = "Morning peak flow normal, breathing clear."
            ),
            RespiratorySymptomRecord(
                timestampMs = now - (oneDay * 2),
                breathlessnessScore = 4,
                wheezing = true,
                cough = true,
                chestTightness = true,
                activityLevel = "Resting",
                sleepDisrupted = true,
                environmentalTriggers = "Cold Air & High Humidity",
                rescueMedicationPuffs = 2,
                peakExpiratoryFlowLpm = 390.0,
                actionPlanZone = ActionPlanZone.YELLOW,
                notes = "Cold air triggered mild wheezing. Used 2 puffs Albuterol; symptoms improved."
            ),
            RespiratorySymptomRecord(
                timestampMs = now - (oneDay * 6),
                breathlessnessScore = 2,
                wheezing = false,
                cough = true,
                chestTightness = false,
                activityLevel = "Resting",
                sleepDisrupted = false,
                environmentalTriggers = "Dust",
                rescueMedicationPuffs = 1,
                peakExpiratoryFlowLpm = 460.0,
                actionPlanZone = ActionPlanZone.GREEN,
                notes = "Mild dry cough after house cleaning."
            )
        )
    }
}
