package com.smarthealth.companion.model

import java.util.UUID

/**
 * Unified Health and Environmental Measurement Model.
 * Represents a discrete physical reading or user input with full provenance and context.
 */
data class Measurement(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String = "default_user",
    val type: MeasurementType,
    val numericValue: Double,
    val secondaryValue: Double? = null, // e.g., diastolic blood pressure
    val unit: String = type.defaultUnit,
    val deviceTimestampMs: Long = 0L,
    val phoneReceiptTimestampMs: Long = System.currentTimeMillis(),
    val source: DataSource = DataSource.WEARABLE,
    val deviceId: String? = null,
    val signalQuality: Int = 100, // 0 - 100%
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Float? = null,
    val locationTimestampMs: Long? = null,
    val activityLabel: String? = null, // e.g., Resting, Walking, Exercise, Sleep
    val symptomNotes: String? = null,
    val dataQualityStatus: DataQualityStatus = DataQualityStatus.GOOD,
    val baselineComparison: BaselineComparison = BaselineComparison.WITHIN_BASELINE,
    val syncStatus: Boolean = false // false = local only, true = synced to cloud
) {
    /**
     * Formats the primary numeric reading into a clean presentation string.
     */
    fun formattedValue(): String {
        return when (type) {
            MeasurementType.HEART_RATE -> "%.0f".format(numericValue)
            MeasurementType.BODY_TEMPERATURE,
            MeasurementType.SENSOR_TEMPERATURE -> "%.1f".format(numericValue)
            MeasurementType.ATMOSPHERIC_PRESSURE -> "%.2f".format(numericValue)
            MeasurementType.ESTIMATED_ALTITUDE -> "%.0f".format(numericValue)
            MeasurementType.SPO2 -> "%.0f".format(numericValue)
            MeasurementType.WEIGHT -> "%.1f".format(numericValue)
            MeasurementType.RESPIRATORY_RATE -> "%.0f".format(numericValue)
            MeasurementType.BLOOD_PRESSURE -> {
                if (secondaryValue != null) {
                    "%.0f/%.0f".format(numericValue, secondaryValue)
                } else {
                    "%.0f".format(numericValue)
                }
            }
        }
    }

    /**
     * Converts Celsius to Fahrenheit if requested by user preference.
     */
    fun toFahrenheit(): Double {
        return (numericValue * 9.0 / 5.0) + 32.0
    }
}
