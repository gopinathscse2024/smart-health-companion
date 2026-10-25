package com.smarthealth.companion.analytics

import com.smarthealth.companion.model.BaselineComparison
import com.smarthealth.companion.model.DataQualityStatus
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.model.UserProfile
import kotlin.math.abs

data class BaselineEvaluation(
    val comparison: BaselineComparison,
    val explanation: String,
    val baselineMedian: Double,
    val deviationScore: Double
)

object BaselineAnalyzer {

    /**
     * Evaluates a measurement against the user's personal historical baseline and clinical thresholds.
     */
    fun evaluateMeasurement(
        measurement: Measurement,
        historicalValidReadings: List<Double>,
        profile: UserProfile
    ): BaselineEvaluation {
        // Step 1: Check signal quality
        if (measurement.dataQualityStatus == DataQualityStatus.POOR ||
            measurement.dataQualityStatus == DataQualityStatus.MISSING_OR_MALFORMED ||
            measurement.signalQuality < 40
        ) {
            return BaselineEvaluation(
                comparison = BaselineComparison.REVIEW_QUALITY,
                explanation = "Sensor quality or motion artifact detected. Please verify sensor placement.",
                baselineMedian = if (historicalValidReadings.isNotEmpty()) StatisticalEngine.calculateMedian(historicalValidReadings) else measurement.numericValue,
                deviationScore = 0.0
            )
        }

        // Step 2: Check for sufficient historical baseline data
        if (historicalValidReadings.size < 10) {
            return BaselineEvaluation(
                comparison = BaselineComparison.INSUFFICIENT_DATA,
                explanation = "Baseline learning in progress (${historicalValidReadings.size}/10 minimum samples recorded).",
                baselineMedian = measurement.numericValue,
                deviationScore = 0.0
            )
        }

        val median = StatisticalEngine.calculateMedian(historicalValidReadings)
        val mad = StatisticalEngine.calculateMAD(historicalValidReadings).coerceAtLeast(1.0)
        val deviation = measurement.numericValue - median
        val deviationScore = abs(deviation) / mad // Robust z-like score via MAD

        // Step 3: Check safety/urgent rules
        when (measurement.type) {
            MeasurementType.HEART_RATE -> {
                if (measurement.numericValue > 150.0 && measurement.activityLabel != "Exercise") {
                    return BaselineEvaluation(
                        comparison = BaselineComparison.URGENT_CONCERN,
                        explanation = "Resting heart rate significantly elevated (%.0f BPM). Rest and seek urgent medical evaluation if persistent or accompanied by chest pain, shortness of breath, or dizziness.".format(measurement.numericValue),
                        baselineMedian = median,
                        deviationScore = deviationScore
                    )
                }
                if (measurement.numericValue < 42.0) {
                    return BaselineEvaluation(
                        comparison = BaselineComparison.URGENT_CONCERN,
                        explanation = "Heart rate very low (%.0f BPM). Review clinical symptoms and contact healthcare professional if lightheaded.".format(measurement.numericValue),
                        baselineMedian = median,
                        deviationScore = deviationScore
                    )
                }
            }

            MeasurementType.BODY_TEMPERATURE -> {
                if (measurement.numericValue >= 39.5) {
                    return BaselineEvaluation(
                        comparison = BaselineComparison.URGENT_CONCERN,
                        explanation = "High body temperature (%.1f °C). Seek medical evaluation for high fever.".format(measurement.numericValue),
                        baselineMedian = median,
                        deviationScore = deviationScore
                    )
                }
                // Ambient desk air temperature when DS18B20 probe is resting on table
                if (measurement.numericValue < 32.0) {
                    return BaselineEvaluation(
                        comparison = BaselineComparison.WITHIN_BASELINE,
                        explanation = "Ambient desk temperature (%.1f °C). Probe off-body standby.".format(measurement.numericValue),
                        baselineMedian = median,
                        deviationScore = 0.0
                    )
                }
                if (measurement.numericValue < 35.0) {
                    return BaselineEvaluation(
                        comparison = BaselineComparison.URGENT_CONCERN,
                        explanation = "Low body temperature (%.1f °C). Check sensor contact and warm up.".format(measurement.numericValue),
                        baselineMedian = median,
                        deviationScore = deviationScore
                    )
                }
            }

            else -> {}
        }

        // Step 4: Check personal baseline bounds
        val isPersonalOutlier = deviationScore > 3.0 // More than 3 MADs from personal median
        val exceedsClinicalTarget = when (measurement.type) {
            MeasurementType.HEART_RATE -> {
                measurement.numericValue < profile.targetHrRestingMin ||
                        measurement.numericValue > profile.targetHrRestingMax
            }
            MeasurementType.BODY_TEMPERATURE -> {
                if (measurement.numericValue < 32.0) false
                else measurement.numericValue < profile.targetBodyTempMin ||
                        measurement.numericValue > profile.targetBodyTempMax
            }
            else -> false
        }

        return if (isPersonalOutlier || exceedsClinicalTarget) {
            val direction = if (deviation > 0) "above" else "below"
            val expl = "Value is %.1f %s personal baseline median of %.1f %s (%.1f MAD deviation).".format(
                abs(deviation),
                measurement.unit,
                median,
                measurement.unit,
                deviationScore
            )
            BaselineEvaluation(
                comparison = BaselineComparison.UNUSUAL_CHANGE,
                explanation = expl,
                baselineMedian = median,
                deviationScore = deviationScore
            )
        } else {
            BaselineEvaluation(
                comparison = BaselineComparison.WITHIN_BASELINE,
                explanation = "Value aligns with personal baseline (median %.1f %s).".format(median, measurement.unit),
                baselineMedian = median,
                deviationScore = deviationScore
            )
        }
    }
}
