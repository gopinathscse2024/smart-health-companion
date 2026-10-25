package com.smarthealth.companion.analytics

import com.smarthealth.companion.model.AlertSeverity
import com.smarthealth.companion.model.AnomalyLevel
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.model.RealtimeAlertItem

/**
 * Intelligent Anomaly Engine that analyzes continuous frequent telemetry streams.
 *
 * Implements a 3-tier classification:
 * - NORMAL: Metrics in healthy physiological/atmospheric range.
 * - MILD_ABNORMAL ("Normal-Abnormal, it's okay"): Minor drift, small changes, or ambient readings.
 *   Action: SILENT (No alarms, no audio tone, no system notification).
 * - OVER_ABNORMAL: Severe threshold breach, critical hypoxic pressure drop (<955 hPa),
 *   extreme tachycardia (>125 BPM), severe fever (>38.8 °C), or sudden rapid surge/plunge.
 *   Action: ACTIVATES alert banner, gentle audio touch, and system notification.
 */
class FrequentDataAnomalyEngine {

    private val pressureWindow = ArrayDeque<Double>(20)
    private val hrWindow = ArrayDeque<Double>(20)
    private val tempWindow = ArrayDeque<Double>(20)

    private var previousAnomalyLevel = AnomalyLevel.NORMAL

    @Synchronized
    fun ingestSample(type: MeasurementType, value: Double) {
        when (type) {
            MeasurementType.ATMOSPHERIC_PRESSURE -> {
                if (pressureWindow.size >= 20) pressureWindow.removeFirst()
                pressureWindow.addLast(value)
            }
            MeasurementType.HEART_RATE -> {
                if (hrWindow.size >= 20) hrWindow.removeFirst()
                hrWindow.addLast(value)
            }
            MeasurementType.BODY_TEMPERATURE -> {
                if (tempWindow.size >= 20) tempWindow.removeFirst()
                tempWindow.addLast(value)
            }
            else -> {}
        }
    }

    @Synchronized
    fun evaluateFrequentData(): AnomalyEvaluationResult {
        val latestPressure = pressureWindow.lastOrNull()
        val latestHr = hrWindow.lastOrNull()
        val latestTemp = tempWindow.lastOrNull()

        // Smoothed rolling medians to filter out transient single-packet sensor jitter
        val smoothPressure = medianOf(pressureWindow)
        val smoothHr = medianOf(hrWindow)
        val smoothTemp = medianOf(tempWindow)

        // 1. Evaluate Pressure Level
        val pressureLevel = evaluatePressureLevel(smoothPressure, pressureWindow)

        // 2. Evaluate Heart Rate Level
        val hrLevel = evaluateHeartRateLevel(smoothHr, hrWindow)

        // 3. Evaluate Temperature Level
        val tempLevel = evaluateTemperatureLevel(smoothTemp, tempWindow)

        // 4. Combined Multi-Parametric Evaluation
        val isHypoxiaCorrelated = (smoothPressure != null && smoothPressure < 968.0) &&
                (smoothHr != null && smoothHr > 115.0)

        val overallLevel = when {
            isHypoxiaCorrelated -> AnomalyLevel.OVER_ABNORMAL
            pressureLevel == AnomalyLevel.OVER_ABNORMAL ||
                    hrLevel == AnomalyLevel.OVER_ABNORMAL ||
                    tempLevel == AnomalyLevel.OVER_ABNORMAL -> AnomalyLevel.OVER_ABNORMAL
            pressureLevel == AnomalyLevel.MILD_ABNORMAL ||
                    hrLevel == AnomalyLevel.MILD_ABNORMAL ||
                    tempLevel == AnomalyLevel.MILD_ABNORMAL -> AnomalyLevel.MILD_ABNORMAL
            else -> AnomalyLevel.NORMAL
        }

        val triggerAlert: RealtimeAlertItem? = if (overallLevel == AnomalyLevel.OVER_ABNORMAL) {
            buildOverAbnormalAlert(
                pressure = latestPressure ?: smoothPressure,
                hr = latestHr ?: smoothHr,
                temp = latestTemp ?: smoothTemp,
                isHypoxia = isHypoxiaCorrelated,
                pressureLevel = pressureLevel,
                hrLevel = hrLevel,
                tempLevel = tempLevel
            )
        } else null

        val wasOverAbnormal = previousAnomalyLevel == AnomalyLevel.OVER_ABNORMAL
        previousAnomalyLevel = overallLevel

        return AnomalyEvaluationResult(
            level = overallLevel,
            pressureLevel = pressureLevel,
            heartRateLevel = hrLevel,
            temperatureLevel = tempLevel,
            triggerAlert = triggerAlert,
            shouldDismissActiveAlert = (wasOverAbnormal && overallLevel != AnomalyLevel.OVER_ABNORMAL)
        )
    }

    private fun evaluatePressureLevel(smoothed: Double?, window: List<Double>): AnomalyLevel {
        if (smoothed == null) return AnomalyLevel.NORMAL

        // Sudden rapid drop check: drop >= 15 hPa within window ending below 968
        val firstInWindow = window.firstOrNull() ?: smoothed
        val suddenDrop = (firstInWindow - smoothed) >= 15.0 && smoothed < 968.0
        val suddenSurge = (smoothed - firstInWindow) >= 18.0 && smoothed > 1038.0

        return when {
            smoothed < 955.0 || smoothed > 1045.0 || suddenDrop || suddenSurge -> AnomalyLevel.OVER_ABNORMAL
            smoothed < 980.0 || smoothed > 1032.0 -> AnomalyLevel.MILD_ABNORMAL // Normal-abnormal, okay (silent)
            else -> AnomalyLevel.NORMAL
        }
    }

    private fun evaluateHeartRateLevel(smoothed: Double?, window: List<Double>): AnomalyLevel {
        if (smoothed == null) return AnomalyLevel.NORMAL

        // Sudden surge check: jump >= 25 BPM within window ending above 115
        val firstInWindow = window.firstOrNull() ?: smoothed
        val suddenSurge = (smoothed - firstInWindow) >= 25.0 && smoothed > 115.0
        val suddenDrop = (firstInWindow - smoothed) >= 30.0 && smoothed < 50.0

        return when {
            smoothed > 125.0 || smoothed < 45.0 || suddenSurge || suddenDrop -> AnomalyLevel.OVER_ABNORMAL
            smoothed > 100.0 || smoothed < 55.0 -> AnomalyLevel.MILD_ABNORMAL // Normal-abnormal, okay (silent)
            else -> AnomalyLevel.NORMAL
        }
    }

    private fun evaluateTemperatureLevel(smoothed: Double?, window: List<Double>): AnomalyLevel {
        if (smoothed == null) return AnomalyLevel.NORMAL

        // If probe is reading ambient desk air (< 32.0 °C), it is off-body standby, NOT abnormal alarm
        if (smoothed < 32.0) return AnomalyLevel.NORMAL

        return when {
            smoothed > 38.8 || smoothed < 34.8 -> AnomalyLevel.OVER_ABNORMAL
            smoothed > 37.5 || smoothed < 35.8 -> AnomalyLevel.MILD_ABNORMAL // Normal-abnormal, okay (silent)
            else -> AnomalyLevel.NORMAL
        }
    }

    private fun buildOverAbnormalAlert(
        pressure: Double?,
        hr: Double?,
        temp: Double?,
        isHypoxia: Boolean,
        pressureLevel: AnomalyLevel,
        hrLevel: AnomalyLevel,
        tempLevel: AnomalyLevel
    ): RealtimeAlertItem {
        return if (isHypoxia || pressureLevel == AnomalyLevel.OVER_ABNORMAL) {
            val pStr = pressure?.let { "%.1f hPa".format(it) } ?: "Abnormal"
            val hrStr = hr?.let { "%.0f BPM".format(it) } ?: "--"
            val tStr = temp?.let { "%.1f °C".format(it) } ?: "--"

            RealtimeAlertItem(
                title = "⚠️ Over-Abnormal Space Pressure & Hypoxia Alert",
                message = "Room pressure is over abnormal ($pStr). Heart rate: $hrStr, Temp: $tStr.",
                guidance = "Move somewhere else, the space has abnormal pressure, your oxygen level is reduced. Please move somewhere else from this. The pressure is more higher and lower. Your oxygen level, heart rate level has abnormal, and temperature goes abnormal. Please move somewhere else.",
                primaryMetric = MeasurementType.ATMOSPHERIC_PRESSURE,
                severity = AlertSeverity.URGENT,
                pressureValue = pressure,
                heartRateValue = hr,
                temperatureValue = temp
            )
        } else if (hrLevel == AnomalyLevel.OVER_ABNORMAL) {
            val hrStr = hr?.let { "%.0f BPM".format(it) } ?: "--"
            RealtimeAlertItem(
                title = "⚠️ Over-Abnormal Heart Rate Anomaly",
                message = "Heart rate is over abnormal ($hrStr). Severe deviation from resting baseline.",
                guidance = "Sudden extreme heart rate surge detected. Please sit down, rest, take deep breaths, and monitor your symptoms immediately.",
                primaryMetric = MeasurementType.HEART_RATE,
                severity = AlertSeverity.URGENT,
                pressureValue = pressure,
                heartRateValue = hr,
                temperatureValue = temp
            )
        } else {
            val tStr = temp?.let { "%.1f °C".format(it) } ?: "--"
            RealtimeAlertItem(
                title = "⚠️ Over-Abnormal Body Temperature Anomaly",
                message = "Core body temperature is over abnormal ($tStr).",
                guidance = "Severe body temperature elevation detected. Hydrate, move to a cool space, and seek medical attention if fever persists.",
                primaryMetric = MeasurementType.BODY_TEMPERATURE,
                severity = AlertSeverity.URGENT,
                pressureValue = pressure,
                heartRateValue = hr,
                temperatureValue = temp
            )
        }
    }

    private fun medianOf(window: List<Double>): Double? {
        if (window.isEmpty()) return null
        val sorted = window.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        }
    }

    @Synchronized
    fun clear() {
        pressureWindow.clear()
        hrWindow.clear()
        tempWindow.clear()
        previousAnomalyLevel = AnomalyLevel.NORMAL
    }
}

data class AnomalyEvaluationResult(
    val level: AnomalyLevel,
    val pressureLevel: AnomalyLevel,
    val heartRateLevel: AnomalyLevel,
    val temperatureLevel: AnomalyLevel,
    val triggerAlert: RealtimeAlertItem?,
    val shouldDismissActiveAlert: Boolean
)
