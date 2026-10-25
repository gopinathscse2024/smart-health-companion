package com.smarthealth.companion.ui.respiratory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthealth.companion.model.ActionPlanZone
import com.smarthealth.companion.model.RespiratorySymptomRecord
import com.smarthealth.companion.theme.StatusAlert
import com.smarthealth.companion.theme.StatusNormal
import com.smarthealth.companion.theme.StatusWarning
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.ui.components.EmergencyAlertBanner
import com.smarthealth.companion.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RespiratoryScreen(viewModel: MainViewModel) {
    val symptomList by viewModel.symptomRecords.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var breathlessnessScore by remember { mutableIntStateOf(1) }
    var wheezing by remember { mutableStateOf(false) }
    var cough by remember { mutableStateOf(false) }
    var chestTightness by remember { mutableStateOf(false) }
    var sleepDisrupted by remember { mutableStateOf(false) }
    var emergencySigns by remember { mutableStateOf(false) }
    var rescuePuffs by remember { mutableIntStateOf(0) }
    var peakFlowInput by remember { mutableStateOf("") }
    var environmentalTrigger by remember { mutableStateOf("None") }
    var notesInput by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // Check emergency red flags
    val hasEmergency = emergencySigns || breathlessnessScore >= 8

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Asthma & COPD Respiratory Tracker",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Log symptoms, peak expiratory flow (PEF), and rescue inhaler usage to correlate with vital signs.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Emergency Warning Banner if severe symptoms exist
        if (hasEmergency) {
            item {
                EmergencyAlertBanner(
                    message = "Severe breathing difficulty or emergency warning signs selected. Seek immediate emergency medical care without delay."
                )
            }
        }

        // Action Plan Status Banner
        item {
            ActionPlanBanner(baselinePef = profile.pefBaselineLpm, currentPef = peakFlowInput.toDoubleOrNull())
        }

        // Symptom Entry Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Record Respiratory Symptom Event",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Breathlessness Slider
                    Text(
                        text = "Breathlessness Score: $breathlessnessScore / 10 (${getBreathlessnessLabel(breathlessnessScore)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (breathlessnessScore >= 7) StatusAlert else MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = breathlessnessScore.toFloat(),
                        onValueChange = { breathlessnessScore = it.toInt() },
                        valueRange = 0f..10f,
                        steps = 9
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Symptoms Checkboxes Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SymptomToggle("Wheeze", wheezing) { wheezing = it }
                        SymptomToggle("Cough", cough) { cough = it }
                        SymptomToggle("Tightness", chestTightness) { chestTightness = it }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Night-time Sleep Disrupted", fontSize = 12.sp)
                        Switch(checked = sleepDisrupted, onCheckedChange = { sleepDisrupted = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Red-flag emergency checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (emergencySigns) StatusAlert.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = emergencySigns, onCheckedChange = { emergencySigns = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Blue/grey lips, confusion, or speech difficulty",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (emergencySigns) StatusAlert else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Peak Expiratory Flow & Rescue Meds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = peakFlowInput,
                            onValueChange = { peakFlowInput = it },
                            label = { Text("PEF (L/min)", fontSize = 11.sp) },
                            placeholder = { Text("e.g. 450", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = rescuePuffs.toString(),
                            onValueChange = { rescuePuffs = it.toIntOrNull() ?: 0 },
                            label = { Text("Rescue Puffs", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes & Triggers (Cold air, pollen, dust, exercise)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val pef = peakFlowInput.toDoubleOrNull()
                            val zone = calculateActionZone(pef, profile.pefBaselineLpm, breathlessnessScore, emergencySigns)
                            viewModel.logSymptomRecord(
                                RespiratorySymptomRecord(
                                    breathlessnessScore = breathlessnessScore,
                                    wheezing = wheezing,
                                    cough = cough,
                                    chestTightness = chestTightness,
                                    sleepDisrupted = sleepDisrupted,
                                    environmentalTriggers = if (notesInput.isNotEmpty()) notesInput else "None recorded",
                                    rescueMedicationPuffs = rescuePuffs,
                                    peakExpiratoryFlowLpm = pef,
                                    hasEmergencySigns = emergencySigns,
                                    actionPlanZone = zone,
                                    notes = notesInput
                                )
                            )
                            // Reset form
                            breathlessnessScore = 1
                            wheezing = false
                            cough = false
                            chestTightness = false
                            emergencySigns = false
                            rescuePuffs = 0
                            peakFlowInput = ""
                            notesInput = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Save Symptom Entry", fontSize = 13.sp)
                    }
                }
            }
        }

        // History of Logged Symptoms
        item {
            Text(
                text = "Recorded Symptom History (${symptomList.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (symptomList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No symptom events logged yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(symptomList) { item ->
                SymptomHistoryCard(record = item)
            }
        }
    }
}

@Composable
fun ActionPlanBanner(baselinePef: Double, currentPef: Double?) {
    val (zoneColor, zoneName, advisory) = when {
        currentPef == null -> Triple(TealPrimary, "Baseline Action Plan (${baselinePef.toInt()} L/min)", "Log your daily peak expiratory flow to monitor your clinical action plan.")
        currentPef >= (baselinePef * 0.8) -> Triple(StatusNormal, "Green Zone (Normal)", "PEF is >80% of baseline. Condition under good control.")
        currentPef >= (baselinePef * 0.5) -> Triple(StatusWarning, "Yellow Zone (Caution)", "PEF is 50-80% of baseline. Follow clinician yellow-zone action plan.")
        else -> Triple(StatusAlert, "Red Zone (Medical Alert)", "PEF is <50% of baseline. Take prescribed rescue medication and seek urgent clinical attention.")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(zoneColor.copy(alpha = 0.12f))
            .border(1.dp, zoneColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(zoneColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = zoneName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = zoneColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = advisory, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SymptomToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SymptomHistoryCard(record: RespiratorySymptomRecord) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val zoneColor = when (record.actionPlanZone) {
        ActionPlanZone.GREEN -> StatusNormal
        ActionPlanZone.YELLOW -> StatusWarning
        ActionPlanZone.RED -> StatusAlert
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = dateFormat.format(Date(record.timestampMs)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(zoneColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = record.actionPlanZone.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = zoneColor)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Breathlessness: ${record.breathlessnessScore}/10 | Rescue Meds: ${record.rescueMedicationPuffs} puffs | PEF: ${record.peakExpiratoryFlowLpm?.toInt() ?: "N/A"} L/min",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!record.notes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Notes: ${record.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun getBreathlessnessLabel(score: Int): String {
    return when (score) {
        0 -> "None"
        in 1..3 -> "Mild"
        in 4..6 -> "Moderate"
        else -> "Severe"
    }
}

private fun calculateActionZone(pef: Double?, baseline: Double, breathlessness: Int, emergency: Boolean): ActionPlanZone {
    if (emergency || breathlessness >= 8) return ActionPlanZone.RED
    if (pef == null) {
        return if (breathlessness >= 5) ActionPlanZone.YELLOW else ActionPlanZone.GREEN
    }
    return when {
        pef >= baseline * 0.8 -> ActionPlanZone.GREEN
        pef >= baseline * 0.5 -> ActionPlanZone.YELLOW
        else -> ActionPlanZone.RED
    }
}
