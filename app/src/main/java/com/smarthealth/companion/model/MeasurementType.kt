package com.smarthealth.companion.model

/**
 * Supported measurement types for the wearable and health companion.
 * Crucially distinguishes BMP180 atmospheric pressure from blood pressure,
 * and BMP180 internal sensor temperature from human body temperature.
 */
enum class MeasurementType(
    val displayName: String,
    val defaultUnit: String,
    val description: String
) {
    HEART_RATE("Heart Rate", "BPM", "Wearable optical photoplethysmography sensor"),
    BODY_TEMPERATURE("Body Temperature", "°C", "Infrared or contact skin/body thermal sensor"),
    ATMOSPHERIC_PRESSURE("Atmospheric Pressure", "hPa", "BMP180 local barometric pressure"),
    SENSOR_TEMPERATURE("BMP180 Sensor Temp", "°C", "Internal silicon temperature of BMP180 sensor"),
    ESTIMATED_ALTITUDE("Estimated Altitude", "m", "Barometric altitude computed from local atmospheric pressure"),
    SPO2("Blood Oxygen (SpO₂)", "%", "Peripheral capillary oxygen saturation (optional sensor)"),
    WEIGHT("Body Weight", "kg", "Body mass from connected scale or manual entry"),
    RESPIRATORY_RATE("Respiratory Rate", "brpm", "Breaths per minute from respiratory sensor or user entry"),
    BLOOD_PRESSURE("Blood Pressure", "mmHg", "Systolic and diastolic pressures from validated monitor")
}
