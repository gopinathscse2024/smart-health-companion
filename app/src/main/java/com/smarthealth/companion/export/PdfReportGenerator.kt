package com.smarthealth.companion.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.smarthealth.companion.model.AnalyticsSummary
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.RespiratorySymptomRecord
import com.smarthealth.companion.model.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun generateReport(
        profile: UserProfile,
        durationLabel: String,
        startDateMs: Long,
        endDateMs: Long,
        summaries: List<AnalyticsSummary>,
        recentMeasurements: List<Measurement>,
        symptomLogs: List<RespiratorySymptomRecord>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 page
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(0, 77, 64) // Deep Teal
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val alertPaint = Paint().apply {
            color = Color.rgb(214, 48, 49)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var y = 45f

        // Title Header
        canvas.drawText("Smart Health Companion AI — Telemetry Report", 40f, y, titlePaint)
        y += 18f

        // Subtitle & Metadata
        canvas.drawText("Report Period: $durationLabel (${dateFormat.format(Date(startDateMs))} to ${dateFormat.format(Date(endDateMs))})", 40f, y, textPaint)
        y += 14f
        canvas.drawText("Subject: ${profile.name} (Age: ${profile.age}) | Generated: ${dateFormat.format(Date())}", 40f, y, textPaint)
        y += 14f
        canvas.drawText("Telemetry Source: ESP32-S3 Wearable (Service UUID: 6e400001-b5a3-f393-e0a9-e50e24dcca9e)", 40f, y, textPaint)
        y += 24f

        // Horizontal Divider
        val linePaint = Paint().apply { color = Color.rgb(226, 232, 240); strokeWidth = 1f }
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Summary Statistics Section
        canvas.drawText("1. STATISTICAL SUMMARY ACROSS MONITORING PERIOD", 40f, y, headerPaint)
        y += 18f

        // Table headers
        canvas.drawText("Metric", 40f, y, headerPaint)
        canvas.drawText("Samples", 160f, y, headerPaint)
        canvas.drawText("Average", 230f, y, headerPaint)
        canvas.drawText("Median", 310f, y, headerPaint)
        canvas.drawText("Min - Max", 390f, y, headerPaint)
        canvas.drawText("Std Dev", 480f, y, headerPaint)
        y += 12f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 14f

        for (s in summaries) {
            canvas.drawText(s.measurementType.displayName, 40f, y, textPaint)
            canvas.drawText("${s.validSamples} (${s.dataCompletenessPercent.toInt()}%)", 160f, y, textPaint)
            canvas.drawText("%.1f %s".format(s.average, s.measurementType.defaultUnit), 230f, y, textPaint)
            canvas.drawText("%.1f %s".format(s.median, s.measurementType.defaultUnit), 310f, y, textPaint)
            canvas.drawText("%.1f - %.1f".format(s.min, s.max), 390f, y, textPaint)
            canvas.drawText("±%.2f".format(s.standardDeviation), 480f, y, textPaint)
            y += 16f
        }

        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Baseline Comparison & Anomalies
        canvas.drawText("2. PERSONAL BASELINE COMPARISONS & FLAGGED READINGS", 40f, y, headerPaint)
        y += 16f

        val anomalousReadings = recentMeasurements.filter {
            it.baselineComparison == com.smarthealth.companion.model.BaselineComparison.UNUSUAL_CHANGE ||
                    it.baselineComparison == com.smarthealth.companion.model.BaselineComparison.URGENT_CONCERN
        }.take(5)

        if (anomalousReadings.isNotEmpty()) {
            for (m in anomalousReadings) {
                val flagText = "[${m.baselineComparison.label}] ${dateFormat.format(Date(m.phoneReceiptTimestampMs))} — ${m.type.displayName}: ${m.formattedValue()} ${m.unit} (Source: ${m.source.label})"
                canvas.drawText(flagText, 40f, y, alertPaint)
                y += 14f
            }
        } else {
            canvas.drawText("No significant statistical baseline outliers recorded during this period.", 40f, y, textPaint)
            y += 14f
        }

        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Respiratory / Asthma Tracking Section
        if (symptomLogs.isNotEmpty()) {
            canvas.drawText("3. RESPIRATORY HEALTH & SYMPTOM LOG (ASTHMA/COPD)", 40f, y, headerPaint)
            y += 16f
            for (r in symptomLogs.take(4)) {
                val logText = "${dateFormat.format(Date(r.timestampMs))} | Zone: ${r.actionPlanZone.name} | Breathlessness: ${r.breathlessnessScore}/10 | PEF: ${r.peakExpiratoryFlowLpm?.toInt() ?: "N/A"} L/min | Rescue Meds: ${r.rescueMedicationPuffs} puffs"
                canvas.drawText(logText, 40f, y, textPaint)
                y += 14f
            }
            y += 15f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f
        }

        // Environmental notes
        canvas.drawText("4. ENVIRONMENTAL & SENSOR PROVENANCE NOTES", 40f, y, headerPaint)
        y += 14f
        canvas.drawText("• BMP180 measures local station atmospheric pressure in hPa. Estimated altitude is derived barometrically.", 40f, y, textPaint)
        y += 12f
        canvas.drawText("• BMP180 sensor temperature represents internal electronics temperature and is strictly segregated from body temperature.", 40f, y, textPaint)
        y += 12f
        canvas.drawText("• Data provenance tags (WEARABLE, MANUAL, SIMULATED) are permanently retained on all records.", 40f, y, textPaint)
        y += 20f

        // Medical Disclaimer
        val disclaimerBoxPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        canvas.drawRect(40f, y, 555f, y + 45f, disclaimerBoxPaint)
        canvas.drawText("CLINICAL DISCLAIMER:", 48f, y + 16f, alertPaint)
        canvas.drawText("This report is compiled for personal health monitoring and clinician consultation only. It does not", 48f, y + 28f, textPaint)
        canvas.drawText("provide a medical diagnosis. In case of medical emergencies, seek immediate clinical care.", 48f, y + 40f, textPaint)

        pdfDocument.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val outputFile = File(reportsDir, "Health_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }
}
