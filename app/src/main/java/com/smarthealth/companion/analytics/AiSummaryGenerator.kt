package com.smarthealth.companion.analytics

import com.smarthealth.companion.model.AnalyticsSummary
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.model.RespiratorySymptomRecord
import com.smarthealth.companion.model.TrendDirection

object AiSummaryGenerator {

    /**
     * Generates a clinically grounded, deterministic plain-language health summary.
     * Strictly factual, adhering to verified data and computed statistics without hallucinations or diagnostic claims.
     */
    fun generateSummary(
        hrSummary: AnalyticsSummary?,
        tempSummary: AnalyticsSummary?,
        pressureSummary: AnalyticsSummary?,
        recentSymptoms: List<RespiratorySymptomRecord>,
        durationLabel: String
    ): String {
        val builder = StringBuilder()
        builder.append("📊 Health & Telemetry Analysis ($durationLabel)\n\n")

        // Heart Rate narrative
        if (hrSummary != null && hrSummary.validSamples > 0) {
            builder.append("• Heart Rate: ")
            builder.append("During the selected $durationLabel period, recorded heart rate averaged %.0f BPM (median: %.0f BPM, range: %.0f - %.0f BPM) across %d valid samples. ".format(
                hrSummary.average,
                hrSummary.median,
                hrSummary.min,
                hrSummary.max,
                hrSummary.validSamples
            ))
            when (hrSummary.trendDirection) {
                TrendDirection.RISING -> builder.append("A mild upward trend of +%.2f BPM/hour was observed. ".format(hrSummary.rateOfChangePerHour))
                TrendDirection.FALLING -> builder.append("A downward trend of %.2f BPM/hour was observed. ".format(hrSummary.rateOfChangePerHour))
                TrendDirection.STABLE -> builder.append("Readings remained consistent with your personal baseline. ")
                TrendDirection.FLUCTUATING -> builder.append("Elevated variability was noted (std dev: %.1f BPM). ".format(hrSummary.standardDeviation))
            }
            if (hrSummary.unusualMeasurementCount > 0) {
                builder.append("Flagged %d readings differing from your personal baseline. ".format(hrSummary.unusualMeasurementCount))
            }
            builder.append("\n\n")
        } else {
            builder.append("• Heart Rate: Insufficient data recorded during this period.\n\n")
        }

        // Body Temperature narrative
        if (tempSummary != null && tempSummary.validSamples > 0) {
            builder.append("• Body Temperature: ")
            builder.append("Average body temperature was %.1f °C (range: %.1f - %.1f °C, %d samples). ".format(
                tempSummary.average,
                tempSummary.min,
                tempSummary.max,
                tempSummary.validSamples
            ))
            if (tempSummary.max >= 38.0) {
                builder.append("Elevated readings above 38.0 °C were recorded; check hydration and review symptoms. ")
            } else {
                builder.append("Thermal readings remained within normal physiological expectations. ")
            }
            builder.append("\n\n")
        }

        // Environmental / Barometric narrative
        if (pressureSummary != null && pressureSummary.validSamples > 0) {
            builder.append("• Environmental Barometric Pressure (BMP180): ")
            builder.append("Local atmospheric station pressure averaged %.1f hPa (range: %.1f - %.1f hPa). ".format(
                pressureSummary.average,
                pressureSummary.min,
                pressureSummary.max
            ))
            if (pressureSummary.standardDeviation > 5.0) {
                builder.append("Significant weather front barometric pressure variation (±%.1f hPa) was observed. ".format(pressureSummary.standardDeviation))
            } else {
                builder.append("Local barometric pressure conditions remained relatively steady. ")
            }
            builder.append("\n\n")
        }

        // Respiratory symptom review
        if (recentSymptoms.isNotEmpty()) {
            val yellowOrRed = recentSymptoms.filter { it.actionPlanZone != com.smarthealth.companion.model.ActionPlanZone.GREEN }
            val rescueUses = recentSymptoms.sumOf { it.rescueMedicationPuffs }
            builder.append("• Respiratory Tracker: ")
            builder.append("Recorded %d symptom entries. ".format(recentSymptoms.size))
            if (rescueUses > 0) {
                builder.append("Total rescue medication recorded: %d puffs. ".format(rescueUses))
            }
            if (yellowOrRed.isNotEmpty()) {
                builder.append("%d logs noted mild-to-moderate symptom increases (Yellow Zone). ".format(yellowOrRed.size))
            }
            builder.append("\n\n")
        }

        // Medical disclaimer
        builder.append("⚠️ Important Clinical Notice: This summary is based solely on recorded sensor telemetry and user logs. It does not constitute a medical diagnosis, clinical evaluation, or treatment recommendation. Always consult a licensed healthcare provider for medical advice.")

        return builder.toString()
    }
}
