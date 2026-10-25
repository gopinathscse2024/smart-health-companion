package com.smarthealth.companion.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smarthealth.companion.model.BaselineComparison
import com.smarthealth.companion.model.DataQualityStatus
import com.smarthealth.companion.model.DataSource
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType

@Entity(
    tableName = "measurements",
    indices = [
        Index(value = ["phoneReceiptTimestampMs"]),
        Index(value = ["type"]),
        Index(value = ["profileId"]),
        Index(value = ["profileId", "type", "phoneReceiptTimestampMs"])
    ]
)
data class MeasurementEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val type: String,
    val numericValue: Double,
    val secondaryValue: Double?,
    val unit: String,
    val deviceTimestampMs: Long,
    val phoneReceiptTimestampMs: Long,
    val source: String,
    val deviceId: String?,
    val signalQuality: Int,
    val latitude: Double?,
    val longitude: Double?,
    val locationAccuracy: Float?,
    val locationTimestampMs: Long?,
    val activityLabel: String?,
    val symptomNotes: String?,
    val dataQualityStatus: String,
    val baselineComparison: String,
    val syncStatus: Boolean
) {
    fun toDomain(): Measurement {
        val measurementType = runCatching { MeasurementType.valueOf(type) }
            .getOrDefault(MeasurementType.HEART_RATE)
        val dataSource = runCatching { DataSource.valueOf(source) }
            .getOrDefault(DataSource.WEARABLE)
        val quality = runCatching { DataQualityStatus.valueOf(dataQualityStatus) }
            .getOrDefault(DataQualityStatus.GOOD)
        val baseline = runCatching { BaselineComparison.valueOf(baselineComparison) }
            .getOrDefault(BaselineComparison.WITHIN_BASELINE)

        return Measurement(
            id = id,
            profileId = profileId,
            type = measurementType,
            numericValue = numericValue,
            secondaryValue = secondaryValue,
            unit = unit,
            deviceTimestampMs = deviceTimestampMs,
            phoneReceiptTimestampMs = phoneReceiptTimestampMs,
            source = dataSource,
            deviceId = deviceId,
            signalQuality = signalQuality,
            latitude = latitude,
            longitude = longitude,
            locationAccuracy = locationAccuracy,
            locationTimestampMs = locationTimestampMs,
            activityLabel = activityLabel,
            symptomNotes = symptomNotes,
            dataQualityStatus = quality,
            baselineComparison = baseline,
            syncStatus = syncStatus
        )
    }

    companion object {
        fun fromDomain(m: Measurement): MeasurementEntity {
            return MeasurementEntity(
                id = m.id,
                profileId = m.profileId,
                type = m.type.name,
                numericValue = m.numericValue,
                secondaryValue = m.secondaryValue,
                unit = m.unit,
                deviceTimestampMs = m.deviceTimestampMs,
                phoneReceiptTimestampMs = m.phoneReceiptTimestampMs,
                source = m.source.name,
                deviceId = m.deviceId,
                signalQuality = m.signalQuality,
                latitude = m.latitude,
                longitude = m.longitude,
                locationAccuracy = m.locationAccuracy,
                locationTimestampMs = m.locationTimestampMs,
                activityLabel = m.activityLabel,
                symptomNotes = m.symptomNotes,
                dataQualityStatus = m.dataQualityStatus.name,
                baselineComparison = m.baselineComparison.name,
                syncStatus = m.syncStatus
            )
        }
    }
}
