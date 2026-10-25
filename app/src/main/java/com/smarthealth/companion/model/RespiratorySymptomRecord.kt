package com.smarthealth.companion.model

import java.util.UUID

/**
 * Respiratory Health and Symptom Log for Asthma and COPD tracking.
 * Provides monitoring and symptom-recording without claiming to diagnose exacerbations.
 */
data class RespiratorySymptomRecord(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String = "default_user",
    val timestampMs: Long = System.currentTimeMillis(),
    val breathlessnessScore: Int = 0, // 0 = None, 1-3 = Mild, 4-6 = Moderate, 7-10 = Severe
    val wheezing: Boolean = false,
    val cough: Boolean = false,
    val chestTightness: Boolean = false,
    val activityLevel: String = "Resting", // Resting, Light Walking, Exercise
    val sleepDisrupted: Boolean = false,
    val environmentalTriggers: String? = null, // e.g. Cold Air, Dust, Pollen, Smoke
    val rescueMedicationPuffs: Int = 0, // e.g., Salbutamol/Albuterol doses
    val peakExpiratoryFlowLpm: Double? = null, // Peak Flow in L/min
    val notes: String? = null,
    // Red-flag emergency indicators requiring immediate medical emergency care
    val hasEmergencySigns: Boolean = false, // Blue/grey lips, severe breathlessness, speech difficulty
    val actionPlanZone: ActionPlanZone = ActionPlanZone.GREEN
)

enum class ActionPlanZone(val title: String, val advisory: String) {
    GREEN("Green Zone (Doing Well)", "Symptoms under control. Continue routine baseline plan."),
    YELLOW("Yellow Zone (Caution)", "Worsening symptoms. Review clinician action plan and rescue inhaler protocol."),
    RED("Red Zone (Urgent Action)", "Severe symptoms. Take prescribed emergency medication and seek medical attention immediately.")
}
