package com.smarthealth.companion

import com.smarthealth.companion.analytics.PredictiveEngine
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PredictiveEngineTest {

    @Test
    fun testInsufficientDataReturnsUnreliable() {
        val fewSamples = (1..5).map { i ->
            Measurement(
                id = UUID.randomUUID().toString(),
                type = MeasurementType.HEART_RATE,
                numericValue = 70.0 + i,
                phoneReceiptTimestampMs = System.currentTimeMillis() - (i * 3600_000L)
            )
        }

        val forecast = PredictiveEngine.generateForecast(fewSamples, MeasurementType.HEART_RATE, 24)
        assertFalse(forecast.isReliable)
        assertEquals("Not enough data to make a reliable prediction.", forecast.trendDescription)
    }

    @Test
    fun testForecastWithSufficientDataGeneratesPredictionAndBounds() {
        val now = System.currentTimeMillis()
        val samples = (1..30).map { i ->
            Measurement(
                id = UUID.randomUUID().toString(),
                type = MeasurementType.HEART_RATE,
                numericValue = 72.0 + (i * 0.1), // Slow upward drift
                phoneReceiptTimestampMs = now - ((30 - i) * 3600_000L)
            )
        }

        val forecast = PredictiveEngine.generateForecast(samples, MeasurementType.HEART_RATE, 24)
        assertTrue(forecast.isReliable)
        assertTrue(forecast.predictedValue > 70.0)
        assertTrue(forecast.uncertaintyLower < forecast.predictedValue)
        assertTrue(forecast.uncertaintyUpper > forecast.predictedValue)
        assertEquals(30, forecast.sampleCountUsed)
    }
}
