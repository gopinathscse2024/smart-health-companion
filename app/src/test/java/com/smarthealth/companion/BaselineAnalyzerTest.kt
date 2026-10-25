package com.smarthealth.companion

import com.smarthealth.companion.analytics.BaselineAnalyzer
import com.smarthealth.companion.model.BaselineComparison
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class BaselineAnalyzerTest {

    private val profile = UserProfile(
        targetHrRestingMin = 50.0,
        targetHrRestingMax = 100.0,
        targetBodyTempMin = 36.0,
        targetBodyTempMax = 37.5
    )

    @Test
    fun testWithinPersonalBaseline() {
        val history = List(25) { 70.0 + (it % 4) } // 70 to 73 BPM
        val m = Measurement(
            id = UUID.randomUUID().toString(),
            type = MeasurementType.HEART_RATE,
            numericValue = 72.0
        )

        val eval = BaselineAnalyzer.evaluateMeasurement(m, history, profile)
        assertEquals(BaselineComparison.WITHIN_BASELINE, eval.comparison)
    }

    @Test
    fun testDetectsUnusualBaselineShift() {
        val history = List(25) { 68.0 } // perfectly flat 68 BPM baseline
        val anomalousM = Measurement(
            id = UUID.randomUUID().toString(),
            type = MeasurementType.HEART_RATE,
            numericValue = 105.0 // Exceeds target & MAD outlier
        )

        val eval = BaselineAnalyzer.evaluateMeasurement(anomalousM, history, profile)
        assertEquals(BaselineComparison.UNUSUAL_CHANGE, eval.comparison)
    }

    @Test
    fun testUrgentConcernTachycardia() {
        val history = List(25) { 72.0 }
        val criticalM = Measurement(
            id = UUID.randomUUID().toString(),
            type = MeasurementType.HEART_RATE,
            numericValue = 165.0, // Resting tachycardia emergency
            activityLabel = "Resting"
        )

        val eval = BaselineAnalyzer.evaluateMeasurement(criticalM, history, profile)
        assertEquals(BaselineComparison.URGENT_CONCERN, eval.comparison)
    }

    @Test
    fun testInsufficientDataClassification() {
        val shortHistory = listOf(70.0, 72.0, 71.0) // Only 3 samples
        val m = Measurement(
            id = UUID.randomUUID().toString(),
            type = MeasurementType.HEART_RATE,
            numericValue = 75.0
        )

        val eval = BaselineAnalyzer.evaluateMeasurement(m, shortHistory, profile)
        assertEquals(BaselineComparison.INSUFFICIENT_DATA, eval.comparison)
    }
}
