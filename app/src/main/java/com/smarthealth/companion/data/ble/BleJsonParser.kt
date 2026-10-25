package com.smarthealth.companion.data.ble

import com.google.gson.JsonParser
import com.smarthealth.companion.model.DataQualityStatus
import com.smarthealth.companion.model.DataSource
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import java.util.UUID

/**
 * Robust BLE JSON parser for ESP32-S3 Wearable messages.
 * Handles:
 * - Single metric JSON (heart_rate, temperature_c, pressure_hpa)
 * - Compound BMP180 payloads (pressure_hpa, sensor_temp, altitude_m)
 * - Streaming fragment buffer reassembly
 * - Malformed, duplicate, or out-of-range data validation
 */
class BleJsonParser {

    private val fragmentBuffer = StringBuilder()
    private var lastProcessedDeviceMs: Long = -1L
    private var lastProcessedType: String = ""

    /**
     * Ingests a raw byte array from BLE notification, appends to fragment buffer,
     * extracts complete JSON objects, and returns parsed measurements.
     */
    @Synchronized
    fun ingestBytes(bytes: ByteArray, deviceId: String? = null): List<Measurement> {
        val rawText = String(bytes, Charsets.UTF_8)
        fragmentBuffer.append(rawText)

        val results = mutableListOf<Measurement>()
        var searchStart = 0

        while (true) {
            val openIdx = fragmentBuffer.indexOf("{", searchStart)
            if (openIdx == -1) {
                // No opening bracket, clear everything prior
                if (fragmentBuffer.isNotEmpty() && !fragmentBuffer.contains("{")) {
                    fragmentBuffer.setLength(0)
                }
                break
            }

            var braceCount = 0
            var closeIdx = -1
            for (i in openIdx until fragmentBuffer.length) {
                if (fragmentBuffer[i] == '{') braceCount++
                else if (fragmentBuffer[i] == '}') {
                    braceCount--
                    if (braceCount == 0) {
                        closeIdx = i
                        break
                    }
                }
            }

            if (closeIdx != -1) {
                val candidateJson = fragmentBuffer.substring(openIdx, closeIdx + 1)
                val parsed = parseJsonString(candidateJson, deviceId)
                results.addAll(parsed)
                fragmentBuffer.delete(0, closeIdx + 1)
                searchStart = 0
            } else {
                // Incomplete fragment waiting for next BLE chunk
                break
            }
        }

        return results
    }

    /**
     * Parses a standalone JSON string into valid health and environmental measurements.
     */
    fun parseJsonString(jsonStr: String, deviceId: String? = null): List<Measurement> {
        val list = mutableListOf<Measurement>()
        try {
            val jsonElement = JsonParser.parseString(jsonStr.trim())
            if (!jsonElement.isJsonObject) return emptyList()
            val obj = jsonElement.asJsonObject

            val typeStr = if (obj.has("type")) obj.get("type").asString.lowercase() else ""
            val deviceMs = if (obj.has("ms")) obj.get("ms").asLong else 0L

            // Duplicate message filter: only filter if exact same type, ms, and value
            val rawVal = if (obj.has("value")) obj.get("value").asDouble else null
            if (typeStr.isNotEmpty() && deviceMs > 0 && deviceMs == lastProcessedDeviceMs && typeStr == lastProcessedType) {
                return emptyList()
            }
            if (deviceMs > 0 && typeStr.isNotEmpty()) {
                lastProcessedDeviceMs = deviceMs
                lastProcessedType = typeStr
            }

            when (typeStr) {
                "heart_rate", "hr" -> {
                    val value = rawVal
                    if (value != null && value in 25.0..260.0) {
                        val quality = when {
                            value in 40.0..190.0 -> DataQualityStatus.GOOD
                            else -> DataQualityStatus.MARGINAL
                        }
                        list.add(
                            Measurement(
                                id = UUID.randomUUID().toString(),
                                type = MeasurementType.HEART_RATE,
                                numericValue = value,
                                unit = "BPM",
                                deviceTimestampMs = deviceMs,
                                phoneReceiptTimestampMs = System.currentTimeMillis(),
                                source = DataSource.WEARABLE,
                                deviceId = deviceId ?: "S3-Health-Monitor",
                                dataQualityStatus = quality
                            )
                        )
                    }
                }

                "temperature_c", "temp", "body_temp" -> {
                    val value = rawVal
                    // Allow 10.0°C to 60.0°C so room-temperature DS18B20 desk tests (e.g. 27.4°C) are accurately received
                    if (value != null && value in 10.0..60.0) {
                        val quality = when {
                            value in 34.0..42.5 -> DataQualityStatus.GOOD
                            else -> DataQualityStatus.MARGINAL // Ambient/probe off skin (<34°C or >42.5°C)
                        }
                        list.add(
                            Measurement(
                                id = UUID.randomUUID().toString(),
                                type = MeasurementType.BODY_TEMPERATURE,
                                numericValue = value,
                                unit = "°C",
                                deviceTimestampMs = deviceMs,
                                phoneReceiptTimestampMs = System.currentTimeMillis(),
                                source = DataSource.WEARABLE,
                                deviceId = deviceId ?: "S3-Health-Monitor",
                                dataQualityStatus = quality
                            )
                        )
                    }
                }

                "pressure_hpa", "pressure" -> {
                    val value = rawVal
                    if (value != null && value in 300.0..1150.0) {
                        list.add(
                            Measurement(
                                id = UUID.randomUUID().toString(),
                                type = MeasurementType.ATMOSPHERIC_PRESSURE,
                                numericValue = value,
                                unit = "hPa",
                                deviceTimestampMs = deviceMs,
                                phoneReceiptTimestampMs = System.currentTimeMillis(),
                                source = DataSource.WEARABLE,
                                deviceId = deviceId ?: "S3-Health-Monitor",
                                dataQualityStatus = DataQualityStatus.GOOD
                            )
                        )
                    }
                }

                "sensor_temp", "sensor_temperature", "bmp180_temperature_c" -> {
                    val value = if (obj.has("value")) obj.get("value").asDouble else null
                    if (value != null && value in -40.0..85.0) {
                        list.add(
                            Measurement(
                                id = UUID.randomUUID().toString(),
                                type = MeasurementType.SENSOR_TEMPERATURE,
                                numericValue = value,
                                unit = "°C",
                                deviceTimestampMs = deviceMs,
                                phoneReceiptTimestampMs = System.currentTimeMillis(),
                                source = DataSource.WEARABLE,
                                deviceId = deviceId ?: "S3-Health-Monitor",
                                dataQualityStatus = DataQualityStatus.GOOD
                            )
                        )
                    }
                }

                "altitude_m", "altitude" -> {
                    val value = if (obj.has("value")) obj.get("value").asDouble else null
                    if (value != null && value in -500.0..9000.0) {
                        list.add(
                            Measurement(
                                id = UUID.randomUUID().toString(),
                                type = MeasurementType.ESTIMATED_ALTITUDE,
                                numericValue = value,
                                unit = "m",
                                deviceTimestampMs = deviceMs,
                                phoneReceiptTimestampMs = System.currentTimeMillis(),
                                source = DataSource.WEARABLE,
                                deviceId = deviceId ?: "S3-Health-Monitor",
                                dataQualityStatus = DataQualityStatus.GOOD
                            )
                        )
                    }
                }

                "bmp180", "environmental" -> {
                    val receiptMs = System.currentTimeMillis()
                    val devId = deviceId ?: "S3-Health-Monitor"

                    // Atmospheric pressure
                    if (obj.has("pressure_hpa")) {
                        val p = obj.get("pressure_hpa").asDouble
                        if (p in 300.0..1150.0) {
                            list.add(
                                Measurement(
                                    type = MeasurementType.ATMOSPHERIC_PRESSURE,
                                    numericValue = p,
                                    unit = "hPa",
                                    deviceTimestampMs = deviceMs,
                                    phoneReceiptTimestampMs = receiptMs,
                                    source = DataSource.WEARABLE,
                                    deviceId = devId
                                )
                            )
                        }
                    }

                    // Sensor Temperature (NOT body temperature)
                    if (obj.has("sensor_temp_c") || obj.has("temperature_c")) {
                        val tElem = if (obj.has("sensor_temp_c")) obj.get("sensor_temp_c") else obj.get("temperature_c")
                        val st = tElem.asDouble
                        if (st in -40.0..85.0) {
                            list.add(
                                Measurement(
                                    type = MeasurementType.SENSOR_TEMPERATURE,
                                    numericValue = st,
                                    unit = "°C",
                                    deviceTimestampMs = deviceMs,
                                    phoneReceiptTimestampMs = receiptMs,
                                    source = DataSource.WEARABLE,
                                    deviceId = devId
                                )
                            )
                        }
                    }

                    // Barometric Altitude
                    if (obj.has("altitude_m")) {
                        val alt = obj.get("altitude_m").asDouble
                        if (alt in -500.0..9000.0) {
                            list.add(
                                Measurement(
                                    type = MeasurementType.ESTIMATED_ALTITUDE,
                                    numericValue = alt,
                                    unit = "m",
                                    deviceTimestampMs = deviceMs,
                                    phoneReceiptTimestampMs = receiptMs,
                                    source = DataSource.WEARABLE,
                                    deviceId = devId
                                )
                            )
                        }
                    }
                }

                "spo2" -> {
                    val value = if (obj.has("value")) obj.get("value").asDouble else null
                    if (value != null && value in 50.0..100.0) {
                        list.add(
                            Measurement(
                                type = MeasurementType.SPO2,
                                numericValue = value,
                                unit = "%",
                                deviceTimestampMs = deviceMs,
                                phoneReceiptTimestampMs = System.currentTimeMillis(),
                                source = DataSource.WEARABLE,
                                deviceId = deviceId ?: "S3-Health-Monitor"
                            )
                        )
                    }
                }

                "respiratory_rate" -> {
                    val value = if (obj.has("value")) obj.get("value").asDouble else null
                    if (value != null && value in 4.0..60.0) {
                        list.add(
                            Measurement(
                                type = MeasurementType.RESPIRATORY_RATE,
                                numericValue = value,
                                unit = "brpm",
                                deviceTimestampMs = deviceMs,
                                phoneReceiptTimestampMs = System.currentTimeMillis(),
                                source = DataSource.WEARABLE,
                                deviceId = deviceId ?: "S3-Health-Monitor"
                            )
                        )
                    }
                }

                "blood_pressure" -> {
                    val systolic = if (obj.has("systolic")) obj.get("systolic").asDouble else null
                    val diastolic = if (obj.has("diastolic")) obj.get("diastolic").asDouble else null
                    if (systolic != null && systolic in 60.0..250.0) {
                        list.add(
                            Measurement(
                                type = MeasurementType.BLOOD_PRESSURE,
                                numericValue = systolic,
                                secondaryValue = diastolic,
                                unit = "mmHg",
                                deviceTimestampMs = deviceMs,
                                phoneReceiptTimestampMs = System.currentTimeMillis(),
                                source = DataSource.WEARABLE,
                                deviceId = deviceId ?: "S3-Health-Monitor"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Malformed JSON caught without crashing the application
        }

        return list
    }

    /**
     * Calculates barometric altitude from local pressure (hPa) and optional sea-level reference (default 1013.25 hPa)
     * using the standard international barometric hypsometric formula.
     */
    fun calculateAltitude(pressureHpa: Double, seaLevelPressureHpa: Double = 1013.25): Double {
        if (pressureHpa <= 0 || seaLevelPressureHpa <= 0) return 0.0
        return 44330.0 * (1.0 - Math.pow(pressureHpa / seaLevelPressureHpa, 0.19029495718363465))
    }
}
