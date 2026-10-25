package com.smarthealth.companion.analytics

import com.smarthealth.companion.model.TriMetricEnvironmentAnalysis
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Tri-Parametric Multi-Metric Engine analyzing Atmospheric Pressure (BMP180),
 * Heart Rate (MAX30102), and Body Temperature (DS18B20) over 24h, 3d, 7d, 14d, 30d.
 *
 * Evaluates ambient room/space barometric conditions, detects environmental hypoxic
 * pressure drops (< 975 hPa) and compensatory heart rate elevation (tachycardia),
 * and generates actionable relocation guidance ("Move somewhere else...").
 */
object TriMetricAnalysisEngine {

    fun analyzeTriMetrics(
        pressureReadings: List<Double>,
        heartRateReadings: List<Double>,
        temperatureReadings: List<Double>,
        durationDays: Int
    ): TriMetricEnvironmentAnalysis {
        val durationLabel = when (durationDays) {
            1 -> "Past 24 Hours"
            else -> "Past $durationDays Days"
        }

        // 1. Atmospheric Pressure Analysis (BMP180)
        val hasPressure = pressureReadings.isNotEmpty()
        val pMean = if (hasPressure) round2(pressureReadings.average()) else 0.0
        val pMin = if (hasPressure) round2(pressureReadings.minOrNull() ?: 0.0) else 0.0
        val pMax = if (hasPressure) round2(pressureReadings.maxOrNull() ?: 0.0) else 0.0

        val pIsLow = hasPressure && (pMean < 965.0 || pMin < 955.0)
        val pIsHigh = hasPressure && (pMean > 1038.0 || pMax > 1045.0)
        val pIsAbnormal = pIsLow || pIsHigh

        val pStatus = when {
            !hasPressure -> "No Pressure Telemetry Recorded"
            pIsLow -> "Abnormally Low Pressure (${pMean} hPa) - Reduced Room Oxygen Pressure"
            pIsHigh -> "Abnormally High Pressure (${pMean} hPa) - High Atmospheric Force"
            else -> "Normal Ambient Pressure (${pMean} hPa, Range: $pMin - $pMax)"
        }

        // 2. Heart Rate Analysis (MAX30102)
        val hasHr = heartRateReadings.isNotEmpty()
        val hrMean = if (hasHr) round1(heartRateReadings.average()) else 0.0
        val hrMin = if (hasHr) round1(heartRateReadings.minOrNull() ?: 0.0) else 0.0
        val hrMax = if (hasHr) round1(heartRateReadings.maxOrNull() ?: 0.0) else 0.0

        val hrIsTachycardia = hasHr && (hrMean > 105.0 || hrMax > 120.0)
        val hrIsBradycardia = hasHr && (hrMean < 48.0 || hrMin < 44.0)
        val hrIsAbnormal = hrIsTachycardia || hrIsBradycardia

        val hrStatus = when {
            !hasHr -> "No Heart Rate Telemetry Recorded"
            hrIsTachycardia -> "Abnormally High Heart Rate (Tachycardia: ${hrMean} BPM, Max: $hrMax)"
            hrIsBradycardia -> "Abnormally Low Heart Rate (Bradycardia: ${hrMean} BPM, Min: $hrMin)"
            else -> "Normal Resting Heart Rate (${hrMean} BPM, Range: $hrMin - $hrMax)"
        }

        // 3. Body Temperature Analysis (DS18B20)
        val hasTemp = temperatureReadings.isNotEmpty()
        val tMean = if (hasTemp) round1(temperatureReadings.average()) else 0.0
        val tMin = if (hasTemp) round1(temperatureReadings.minOrNull() ?: 0.0) else 0.0
        val tMax = if (hasTemp) round1(temperatureReadings.maxOrNull() ?: 0.0) else 0.0

        val isAmbientDeskProbe = hasTemp && tMean < 32.0 // Probe left on desk measuring room temperature
        val tIsFebrile = hasTemp && (tMean > 37.8 || tMax > 38.5)
        val tIsSubnormal = hasTemp && !isAmbientDeskProbe && (tMean < 35.0 || tMin < 34.5)
        val tIsAbnormal = tIsFebrile || tIsSubnormal

        val tStatus = when {
            !hasTemp -> "No Temperature Telemetry Recorded"
            isAmbientDeskProbe -> "Ambient Probe / Off-Body Standby (${tMean} °C, Desk Room Temp)"
            tIsFebrile -> "Abnormally High Core Temperature (Fever: ${tMean} °C, Peak: $tMax)"
            tIsSubnormal -> "Subnormal Core Temperature (${tMean} °C, Min: $tMin)"
            else -> "Normal Body Temperature (${tMean} °C, Range: $tMin - $tMax)"
        }

        // 4. Combined Multi-Parametric Evaluation & Relocation Guidance
        val hasAnyData = hasPressure || hasHr || hasTemp
        val isOverallAbnormal = pIsAbnormal || hrIsAbnormal || tIsAbnormal

        val overallLevel: String
        val clinicalGuidance: String
        val narrative: String

        if (!hasAnyData) {
            overallLevel = "Pending Hardware Telemetry"
            clinicalGuidance = "Connect ESP32-S3 wearable monitor via Bluetooth to stream atmospheric pressure, heart rate, and body temperature."
            narrative = "No physical telemetry recorded during the $durationLabel window."
        } else if (pIsLow && hrIsTachycardia) {
            overallLevel = "Environmental Hypoxia & Compensatory Tachycardia"
            clinicalGuidance = "⚠️ Move somewhere else immediately: The space has abnormally low atmospheric pressure and reduced oxygen availability. Your heart rate is elevated compensating for oxygen deficit. Please move somewhere else from this room or area."
            narrative = buildString {
                append("Over the $durationLabel, the BMP180 sensor measured ambient room pressure averaging $pMean hPa (minimum: $pMin hPa). ")
                append("Atmospheric pressure below normal sea-level threshold (980 hPa) reduces ambient oxygen partial pressure (pO2), making breathing more demanding. ")
                append("Simultaneously, the body's heart rate elevated to an average of $hrMean BPM (peak: $hrMax BPM) as a physiological compensatory mechanism to maintain tissue oxygenation. ")
                append("Body temperature averaged $tMean °C. ")
                append("Recommendation: Move somewhere else with adequate ventilation, fresh airflow, or normalized atmospheric pressure.")
            }
        } else if (pIsLow) {
            overallLevel = "Abnormally Low Space Pressure (Hypoxia Risk)"
            clinicalGuidance = "⚠️ Move somewhere else: The space has abnormally low atmospheric pressure ($pMean hPa). Oxygen partial pressure is reduced. Please move somewhere else from this enclosed space."
            narrative = "The room or area had persistently low atmospheric pressure averaging $pMean hPa over $durationLabel. Vital signs: Heart rate $hrMean BPM, Temperature $tMean °C. Ensure proper airflow or relocate."
        } else if (pIsHigh) {
            overallLevel = "Abnormally High Environmental Pressure"
            clinicalGuidance = "⚠️ Move somewhere else: The ambient pressure ($pMean hPa) is higher than normal atmospheric baseline. Please ventilate or move to an ambient space."
            narrative = "Room pressure elevated to $pMean hPa (maximum: $pMax hPa) over $durationLabel. Vitals: Heart rate $hrMean BPM, Temperature $tMean °C."
        } else if (hrIsAbnormal || tIsAbnormal) {
            overallLevel = "Abnormal Body Vitals Detected"
            clinicalGuidance = if (hasPressure) {
                "Room atmospheric pressure is normal ($pMean hPa), but body vitals are abnormal (Heart Rate: $hrMean BPM, Temp: $tMean °C). Rest, hydrate, and monitor your symptoms."
            } else {
                "Body vitals are abnormal (Heart Rate: $hrMean BPM, Temp: $tMean °C). Rest, hydrate, and monitor your symptoms."
            }
            narrative = if (hasPressure) {
                "Atmospheric pressure remained at standard levels ($pMean hPa). However, physiological vitals exhibited anomalies with heart rate at $hrMean BPM and body temperature at $tMean °C."
            } else {
                "Physiological vitals exhibited anomalies with heart rate averaging $hrMean BPM and body temperature averaging $tMean °C over $durationLabel."
            }
        } else if (isAmbientDeskProbe) {
            overallLevel = "Ambient Standby (Probe Off-Body)"
            clinicalGuidance = if (hasPressure) {
                "Room atmospheric pressure is normal ($pMean hPa). Temperature probe is measuring ambient desk room temperature ($tMean °C). Attach probe to body for clinical vital tracking."
            } else {
                "Temperature probe is measuring ambient room temperature ($tMean °C). Attach probe to body for clinical vital tracking."
            }
            narrative = if (hasPressure) {
                "Room atmospheric pressure averaged $pMean hPa over $durationLabel. DS18B20 probe recorded ambient room air at $tMean °C on standby."
            } else {
                "DS18B20 probe recorded ambient room air at $tMean °C on standby over $durationLabel."
            }
        } else {
            overallLevel = "Optimal Environmental & Physiological Equilibrium"
            clinicalGuidance = if (hasPressure) {
                "Atmospheric pressure, oxygen availability, heart rate, and body temperature all within healthy baseline limits."
            } else {
                "Body heart rate and temperature are within healthy baseline limits."
            }
            narrative = if (hasPressure) {
                "Over the $durationLabel, room atmospheric pressure averaged $pMean hPa (ideal sea-level baseline). Body heart rate averaged $hrMean BPM and temperature averaged $tMean °C. The person and ambient room environment maintained stable equilibrium."
            } else {
                "Over the $durationLabel, body heart rate averaged $hrMean BPM and temperature averaged $tMean °C within stable equilibrium."
            }
        }

        return TriMetricEnvironmentAnalysis(
            durationDays = durationDays,
            durationLabel = durationLabel,
            hasPressureData = hasPressure,
            pressureMean = pMean,
            pressureMin = pMin,
            pressureMax = pMax,
            pressureIsAbnormal = pIsAbnormal,
            pressureStatusDescription = pStatus,
            hasHeartRateData = hasHr,
            heartRateMean = hrMean,
            heartRateMin = hrMin,
            heartRateMax = hrMax,
            heartRateIsAbnormal = hrIsAbnormal,
            heartRateStatusDescription = hrStatus,
            hasTemperatureData = hasTemp,
            temperatureMean = tMean,
            temperatureMin = tMin,
            temperatureMax = tMax,
            temperatureIsAbnormal = tIsAbnormal,
            temperatureStatusDescription = tStatus,
            overallLevel = overallLevel,
            isOverallAbnormal = isOverallAbnormal,
            clinicalGuidance = clinicalGuidance,
            detailedNarrative = narrative
        )
    }

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
    private fun round2(v: Double): Double = (v * 100.0).roundToInt() / 100.0
}
