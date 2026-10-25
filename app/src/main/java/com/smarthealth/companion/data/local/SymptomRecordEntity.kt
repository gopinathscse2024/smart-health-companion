package com.smarthealth.companion.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smarthealth.companion.model.ActionPlanZone
import com.smarthealth.companion.model.RespiratorySymptomRecord

@Entity(
    tableName = "symptom_records",
    indices = [
        Index(value = ["timestampMs"]),
        Index(value = ["profileId"])
    ]
)
data class SymptomRecordEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val timestampMs: Long,
    val breathlessnessScore: Int,
    val wheezing: Boolean,
    val cough: Boolean,
    val chestTightness: Boolean,
    val activityLevel: String,
    val sleepDisrupted: Boolean,
    val environmentalTriggers: String?,
    val rescueMedicationPuffs: Int,
    val peakExpiratoryFlowLpm: Double?,
    val notes: String?,
    val hasEmergencySigns: Boolean,
    val actionPlanZone: String
) {
    fun toDomain(): RespiratorySymptomRecord {
        val zone = runCatching { ActionPlanZone.valueOf(actionPlanZone) }
            .getOrDefault(ActionPlanZone.GREEN)
        return RespiratorySymptomRecord(
            id = id,
            profileId = profileId,
            timestampMs = timestampMs,
            breathlessnessScore = breathlessnessScore,
            wheezing = wheezing,
            cough = cough,
            chestTightness = chestTightness,
            activityLevel = activityLevel,
            sleepDisrupted = sleepDisrupted,
            environmentalTriggers = environmentalTriggers,
            rescueMedicationPuffs = rescueMedicationPuffs,
            peakExpiratoryFlowLpm = peakExpiratoryFlowLpm,
            notes = notes,
            hasEmergencySigns = hasEmergencySigns,
            actionPlanZone = zone
        )
    }

    companion object {
        fun fromDomain(r: RespiratorySymptomRecord): SymptomRecordEntity {
            return SymptomRecordEntity(
                id = r.id,
                profileId = r.profileId,
                timestampMs = r.timestampMs,
                breathlessnessScore = r.breathlessnessScore,
                wheezing = r.wheezing,
                cough = r.cough,
                chestTightness = r.chestTightness,
                activityLevel = r.activityLevel,
                sleepDisrupted = r.sleepDisrupted,
                environmentalTriggers = r.environmentalTriggers,
                rescueMedicationPuffs = r.rescueMedicationPuffs,
                peakExpiratoryFlowLpm = r.peakExpiratoryFlowLpm,
                notes = r.notes,
                hasEmergencySigns = r.hasEmergencySigns,
                actionPlanZone = r.actionPlanZone.name
            )
        }
    }
}
