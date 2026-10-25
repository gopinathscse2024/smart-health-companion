package com.smarthealth.companion.export

import android.content.Context
import com.smarthealth.companion.model.Measurement
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter(private val context: Context) {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    fun exportToCsv(measurements: List<Measurement>): File {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, "health_telemetry_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            // CSV Header
            writer.append("MeasurementID,TimestampMs,DateTimeISO,MeasurementType,NumericValue,SecondaryValue,Unit,Source,DeviceId,SignalQuality,Latitude,Longitude,DataQualityStatus,BaselineComparison,ActivityLabel,SymptomNotes\n")

            for (m in measurements) {
                val line = buildString {
                    append(escape(m.id)).append(",")
                    append(m.phoneReceiptTimestampMs).append(",")
                    append(escape(isoFormat.format(Date(m.phoneReceiptTimestampMs)))).append(",")
                    append(escape(m.type.name)).append(",")
                    append(m.numericValue).append(",")
                    append(m.secondaryValue ?: "").append(",")
                    append(escape(m.unit)).append(",")
                    append(escape(m.source.name)).append(",")
                    append(escape(m.deviceId ?: "")).append(",")
                    append(m.signalQuality).append(",")
                    append(m.latitude ?: "").append(",")
                    append(m.longitude ?: "").append(",")
                    append(escape(m.dataQualityStatus.name)).append(",")
                    append(escape(m.baselineComparison.name)).append(",")
                    append(escape(m.activityLabel ?: "")).append(",")
                    append(escape(m.symptomNotes ?: ""))
                    append("\n")
                }
                writer.append(line)
            }
        }
        return file
    }

    private fun escape(text: String): String {
        return if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }
}
