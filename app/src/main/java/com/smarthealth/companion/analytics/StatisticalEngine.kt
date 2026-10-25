package com.smarthealth.companion.analytics

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Robust statistical computing engine for health and environmental signals.
 * Implements parametric and non-parametric statistics (Median, MAD, IQR, Pearson Correlation, Linear Regression).
 */
object StatisticalEngine {

    fun calculateMean(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        return values.average()
    }

    fun calculateMedian(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val size = sorted.size
        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2.0
        } else {
            sorted[size / 2]
        }
    }

    fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val sumSquaredDiff = values.sumOf { (it - mean) * (it - mean) }
        return sqrt(sumSquaredDiff / (values.size - 1))
    }

    fun calculatePercentile(values: List<Double>, percentile: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        if (sorted.size == 1) return sorted.first()
        val index = (percentile / 100.0) * (sorted.size - 1)
        val lower = index.toInt()
        val fraction = index - lower
        return if (lower + 1 < sorted.size) {
            sorted[lower] + fraction * (sorted[lower + 1] - sorted[lower])
        } else {
            sorted[lower]
        }
    }

    /**
     * Median Absolute Deviation (MAD) scaled by 1.4826 for normal distribution consistency.
     * Highly resilient to sensor noise and transient outliers compared to standard deviation.
     */
    fun calculateMAD(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val median = calculateMedian(values)
        val deviations = values.map { abs(it - median) }
        val rawMad = calculateMedian(deviations)
        return rawMad * 1.4826
    }

    /**
     * Pearson product-moment correlation coefficient between two time-aligned signal series.
     * Returns r in [-1.0, 1.0], or 0.0 if variance is zero or lists differ in size.
     */
    fun calculateCorrelation(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size || x.size < 3) return 0.0
        val meanX = x.average()
        val meanY = y.average()

        var num = 0.0
        var denX = 0.0
        var denY = 0.0

        for (i in x.indices) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }

        val denom = sqrt(denX * denY)
        return if (denom > 1e-9) num / denom else 0.0
    }

    /**
     * Ordinary Least Squares linear regression.
     * Returns Pair(slope, intercept).
     */
    fun calculateLinearRegression(x: List<Double>, y: List<Double>): Pair<Double, Double> {
        if (x.size != y.size || x.size < 2) return Pair(0.0, if (y.isNotEmpty()) y.first() else 0.0)

        val n = x.size
        val meanX = x.average()
        val meanY = y.average()

        var numerator = 0.0
        var denominator = 0.0

        for (i in 0 until n) {
            val dx = x[i] - meanX
            numerator += dx * (y[i] - meanY)
            denominator += dx * dx
        }

        val slope = if (denominator > 1e-9) numerator / denominator else 0.0
        val intercept = meanY - (slope * meanX)
        return Pair(slope, intercept)
    }

    /**
     * Exponentially Weighted Moving Average (EWMA) with smoothing factor alpha.
     */
    fun calculateEWMA(values: List<Double>, alpha: Double = 0.2): List<Double> {
        if (values.isEmpty()) return emptyList()
        val smoothed = ArrayList<Double>(values.size)
        var s = values[0]
        smoothed.add(s)

        for (i in 1 until values.size) {
            s = alpha * values[i] + (1.0 - alpha) * s
            smoothed.add(s)
        }
        return smoothed
    }
}
