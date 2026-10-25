package com.smarthealth.companion

import com.smarthealth.companion.analytics.StatisticalEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticalEngineTest {

    @Test
    fun testMeanAndMedianCalculation() {
        val oddList = listOf(60.0, 70.0, 80.0, 90.0, 100.0)
        assertEquals(80.0, StatisticalEngine.calculateMean(oddList), 0.001)
        assertEquals(80.0, StatisticalEngine.calculateMedian(oddList), 0.001)

        val evenList = listOf(60.0, 70.0, 80.0, 90.0)
        assertEquals(75.0, StatisticalEngine.calculateMean(evenList), 0.001)
        assertEquals(75.0, StatisticalEngine.calculateMedian(evenList), 0.001)
    }

    @Test
    fun testStandardDeviation() {
        val values = listOf(10.0, 12.0, 23.0, 23.0, 16.0, 23.0, 21.0, 16.0)
        val stdDev = StatisticalEngine.calculateStandardDeviation(values)
        // Expected sample standard deviation ≈ 5.237
        assertEquals(5.237, stdDev, 0.01)
    }

    @Test
    fun testPercentilesAndIQR() {
        val values = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0)
        val p25 = StatisticalEngine.calculatePercentile(values, 25.0)
        val p75 = StatisticalEngine.calculatePercentile(values, 75.0)
        assertEquals(3.0, p25, 0.01)
        assertEquals(7.0, p75, 0.01)
        val iqr = p75 - p25
        assertEquals(4.0, iqr, 0.01)
    }

    @Test
    fun testMADCalculation() {
        val normalValues = listOf(70.0, 71.0, 72.0, 71.5, 73.0, 70.5, 72.0)
        val mad = StatisticalEngine.calculateMAD(normalValues)
        assertTrue(mad > 0.0)
    }

    @Test
    fun testPearsonCorrelation() {
        // Perfect positive correlation
        val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val y = listOf(2.0, 4.0, 6.0, 8.0, 10.0)
        val r = StatisticalEngine.calculateCorrelation(x, y)
        assertEquals(1.0, r, 0.001)

        // Perfect inverse correlation
        val yNeg = listOf(10.0, 8.0, 6.0, 4.0, 2.0)
        val rNeg = StatisticalEngine.calculateCorrelation(x, yNeg)
        assertEquals(-1.0, rNeg, 0.001)
    }

    @Test
    fun testLinearRegression() {
        val x = listOf(0.0, 1.0, 2.0, 3.0, 4.0)
        val y = listOf(50.0, 55.0, 60.0, 65.0, 70.0)
        val (slope, intercept) = StatisticalEngine.calculateLinearRegression(x, y)
        assertEquals(5.0, slope, 0.001)
        assertEquals(50.0, intercept, 0.001)
    }

    @Test
    fun testEWMA() {
        val values = listOf(10.0, 20.0, 30.0, 40.0)
        val ewma = StatisticalEngine.calculateEWMA(values, alpha = 0.5)
        assertEquals(4, ewma.size)
        assertEquals(10.0, ewma[0], 0.001)
        assertEquals(15.0, ewma[1], 0.001) // 0.5 * 20 + 0.5 * 10 = 15
        assertEquals(22.5, ewma[2], 0.001) // 0.5 * 30 + 0.5 * 15 = 22.5
    }
}
