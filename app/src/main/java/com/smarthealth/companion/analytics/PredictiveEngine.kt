package com.smarthealth.companion.analytics

import com.smarthealth.companion.model.ForecastResult
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import kotlin.math.sqrt

object PredictiveEngine {

    /**
     * Computes on-device statistical prediction for the selected horizon.
     * Supported horizons:
     * - 6 hours
     * - 24 hours
     * - 72 hours (3 days)
     * - 168 hours (7 days)
     */
    fun generateForecast(
        measurements: List<Measurement>,
        type: MeasurementType,
        horizonHours: Int
    ): ForecastResult {
        val validSamples = measurements
            .filter { it.type == type && it.numericValue > 0 }
            .sortedBy { it.phoneReceiptTimestampMs }

        val horizonLabel = when (horizonHours) {
            6 -> "Next 6 Hours"
            24 -> "Next 24 Hours"
            72 -> "Next 3 Days"
            168 -> "Next 7 Days"
            else -> "Next $horizonHours Hours"
        }

        // Check for minimum data requirement
        if (validSamples.size < 15) {
            return ForecastResult(
                measurementType = type,
                historicalPeriodUsed = "${validSamples.size} recorded samples",
                forecastHorizonHours = horizonHours,
                horizonLabel = horizonLabel,
                currentLatestValue = validSamples.lastOrNull()?.numericValue ?: 0.0,
                predictedValue = validSamples.lastOrNull()?.numericValue ?: 0.0,
                uncertaintyLower = validSamples.lastOrNull()?.numericValue ?: 0.0,
                uncertaintyUpper = validSamples.lastOrNull()?.numericValue ?: 0.0,
                trendDescription = "Not enough data to make a reliable prediction.",
                modelMethod = "Insufficient Historical Data",
                sampleCountUsed = validSamples.size,
                dataQualityScore = 0.0,
                contributingFactors = listOf(
                    "Minimum of 15 valid samples required to compute trend projection",
                    "Currently available valid samples: ${validSamples.size}"
                ),
                isReliable = false
            )
        }

        val values = validSamples.map { it.numericValue }
        val timestamps = validSamples.map { it.phoneReceiptTimestampMs }
        val firstTime = timestamps.first()
        val timeHours = timestamps.map { (it - firstTime).toDouble() / 3600_000.0 }

        val latestValue = values.last()
        val mean = StatisticalEngine.calculateMean(values)
        val stdDev = StatisticalEngine.calculateStandardDeviation(values)

        // Linear regression over time
        val (slopePerHour, intercept) = StatisticalEngine.calculateLinearRegression(timeHours, values)

        val latestTimeHour = timeHours.last()
        val targetTimeHour = latestTimeHour + horizonHours.toDouble()
        val linearPrediction = intercept + (slopePerHour * targetTimeHour)

        // Exponential smoothing baseline for damping long extrapolations
        val ewmaList = StatisticalEngine.calculateEWMA(values, alpha = 0.25)
        val ewmaLatest = ewmaList.last()

        // Weighted blend between linear trend and recent exponential smoothed baseline
        // Damps steep slopes over long horizons (e.g. 7 days) to avoid physically impossible vital signs
        val blendWeight = (1.0 / (1.0 + horizonHours / 24.0)).coerceIn(0.2, 0.9)
        var blendedPrediction = (linearPrediction * blendWeight) + (ewmaLatest * (1.0 - blendWeight))

        // Physiological safety clamping
        when (type) {
            MeasurementType.HEART_RATE -> blendedPrediction = blendedPrediction.coerceIn(38.0, 210.0)
            MeasurementType.BODY_TEMPERATURE -> blendedPrediction = blendedPrediction.coerceIn(34.0, 42.0)
            MeasurementType.ATMOSPHERIC_PRESSURE -> blendedPrediction = blendedPrediction.coerceIn(900.0, 1080.0)
            MeasurementType.SPO2 -> blendedPrediction = blendedPrediction.coerceIn(80.0, 100.0)
            else -> {}
        }

        // Uncertainty interval calculation based on residual variance and projection distance
        val residuals = values.zip(timeHours).map { (v, t) ->
            val expected = intercept + (slopePerHour * t)
            (v - expected) * (v - expected)
        }
        val residualVariance = if (residuals.size > 2) residuals.sum() / (residuals.size - 2) else stdDev * stdDev
        val standardError = sqrt(residualVariance.coerceAtLeast(0.01))

        // Distance penalty factor: uncertainty widens as forecast horizon increases
        val distanceFactor = sqrt(1.0 + (horizonHours.toDouble() / 24.0))
        val marginOfError = 1.96 * standardError * distanceFactor // 95% statistical confidence interval

        val lowerBound = (blendedPrediction - marginOfError).coerceAtLeast(0.0)
        val upperBound = blendedPrediction + marginOfError

        val trendDirection = when {
            slopePerHour > 0.05 -> "projected upward trend (+%.2f %s/hr)".format(slopePerHour, type.defaultUnit)
            slopePerHour < -0.05 -> "projected downward trend (%.2f %s/hr)".format(slopePerHour, type.defaultUnit)
            else -> "projected stable trend within recent baseline variability"
        }

        val factors = mutableListOf<String>()
        factors.add("Linear regression slope: %.3f %s per hour".format(slopePerHour, type.defaultUnit))
        factors.add("Exponential smoothed recent baseline: %.1f %s".format(ewmaLatest, type.defaultUnit))
        factors.add("Rolling sample standard deviation: %.2f %s".format(stdDev, type.defaultUnit))
        factors.add("Analysis window: %.1f days of recorded data".format((timestamps.last() - timestamps.first()) / 86400_000.0))

        val periodLabel = "%.1f days (%d samples)".format(
            (timestamps.last() - timestamps.first()) / 86400_000.0,
            validSamples.size
        )

        return ForecastResult(
            measurementType = type,
            historicalPeriodUsed = periodLabel,
            forecastHorizonHours = horizonHours,
            horizonLabel = horizonLabel,
            currentLatestValue = latestValue,
            predictedValue = blendedPrediction,
            uncertaintyLower = lowerBound,
            uncertaintyUpper = upperBound,
            trendDescription = trendDirection,
            modelMethod = "Damped Linear Regression + EWMA (95% CI)",
            sampleCountUsed = validSamples.size,
            dataQualityScore = 95.0,
            contributingFactors = factors,
            isReliable = true
        )
    }
}
