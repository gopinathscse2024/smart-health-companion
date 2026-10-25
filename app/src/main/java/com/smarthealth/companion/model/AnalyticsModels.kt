package com.smarthealth.companion.model

enum class TrendDirection(val label: String) {
    RISING("Trending Upward"),
    FALLING("Trending Downward"),
    STABLE("Stable / Baseline Consistent"),
    FLUCTUATING("High Variability")
}

data class AnalyticsSummary(
    val measurementType: MeasurementType,
    val startDateMs: Long,
    val endDateMs: Long,
    val durationLabel: String,
    val totalRecordedSamples: Int,
    val validSamples: Int,
    val missingOrMalformedSamples: Int,
    val average: Double,
    val median: Double,
    val min: Double,
    val max: Double,
    val standardDeviation: Double,
    val percentile25: Double,
    val percentile75: Double,
    val iqr: Double,
    val baselineMedian: Double,
    val baselineShiftPercent: Double,
    val trendDirection: TrendDirection,
    val rateOfChangePerHour: Double,
    val unusualMeasurementCount: Int,
    val unusualDurationMinutes: Long,
    val dataCompletenessPercent: Double,
    val confidenceLevel: String
)

data class ForecastResult(
    val measurementType: MeasurementType,
    val historicalPeriodUsed: String,
    val forecastHorizonHours: Int,
    val horizonLabel: String,
    val currentLatestValue: Double,
    val predictedValue: Double,
    val uncertaintyLower: Double,
    val uncertaintyUpper: Double,
    val trendDescription: String,
    val modelMethod: String,
    val sampleCountUsed: Int,
    val dataQualityScore: Double,
    val contributingFactors: List<String>,
    val isReliable: Boolean,
    val disclaimer: String = "Statistical projection for wellness awareness only. Not a medical diagnosis or treatment plan."
)

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int = -60,
    val isS3Wearable: Boolean = false
)

data class BleCommandLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val success: Boolean,
    val details: String = ""
)

data class UserProfile(
    val profileId: String = "default_user",
    val name: String = "Primary User",
    val age: Int = 32,
    val gender: String = "Not Specified",
    val targetHrRestingMin: Double = 55.0,
    val targetHrRestingMax: Double = 90.0,
    val targetBodyTempMin: Double = 36.1,
    val targetBodyTempMax: Double = 37.5,
    val hasAsthmaOrCopd: Boolean = true,
    val pefBaselineLpm: Double = 480.0,
    val enableGpsLogging: Boolean = false,
    val useFahrenheit: Boolean = false,
    val simulationModeEnabled: Boolean = false
)

enum class AnomalyLevel(val label: String) {
    NORMAL("Normal"),
    MILD_ABNORMAL("Mild Variation"),
    OVER_ABNORMAL("Over Abnormal / Critical")
}

enum class AlertSeverity {
    INFO,
    WARNING,
    URGENT
}

data class RealtimeAlertItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val guidance: String,
    val primaryMetric: MeasurementType,
    val severity: AlertSeverity = AlertSeverity.WARNING,
    val timestampMs: Long = System.currentTimeMillis(),
    val pressureValue: Double? = null,
    val heartRateValue: Double? = null,
    val temperatureValue: Double? = null
)

data class TriMetricEnvironmentAnalysis(
    val durationDays: Int,
    val durationLabel: String,
    // Atmospheric Pressure (BMP180)
    val hasPressureData: Boolean = false,
    val pressureMean: Double = 0.0,
    val pressureMin: Double = 0.0,
    val pressureMax: Double = 0.0,
    val pressureIsAbnormal: Boolean = false,
    val pressureStatusDescription: String = "No Pressure Data",
    // Heart Rate (MAX30102)
    val hasHeartRateData: Boolean = false,
    val heartRateMean: Double = 0.0,
    val heartRateMin: Double = 0.0,
    val heartRateMax: Double = 0.0,
    val heartRateIsAbnormal: Boolean = false,
    val heartRateStatusDescription: String = "No Heart Rate Data",
    // Body Temperature (DS18B20)
    val hasTemperatureData: Boolean = false,
    val temperatureMean: Double = 0.0,
    val temperatureMin: Double = 0.0,
    val temperatureMax: Double = 0.0,
    val temperatureIsAbnormal: Boolean = false,
    val temperatureStatusDescription: String = "No Temperature Data",
    // Combined Multi-Parametric Evaluation
    val overallLevel: String = "Pending Telemetry",
    val isOverallAbnormal: Boolean = false,
    val clinicalGuidance: String = "Connect ESP32-S3 wearable monitor to collect room pressure, heart rate, and temperature.",
    val detailedNarrative: String = ""
)
