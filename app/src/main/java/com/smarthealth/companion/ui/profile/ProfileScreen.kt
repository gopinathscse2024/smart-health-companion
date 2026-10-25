package com.smarthealth.companion.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val currentProfile by viewModel.userProfile.collectAsState()

    var nameInput by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var ageInput by remember(currentProfile) { mutableStateOf(currentProfile.age.toString()) }
    var hrMinInput by remember(currentProfile) { mutableStateOf(currentProfile.targetHrRestingMin.toInt().toString()) }
    var hrMaxInput by remember(currentProfile) { mutableStateOf(currentProfile.targetHrRestingMax.toInt().toString()) }
    var tempMinInput by remember(currentProfile) { mutableStateOf(String.format(java.util.Locale.US, "%.1f", currentProfile.targetBodyTempMin)) }
    var tempMaxInput by remember(currentProfile) { mutableStateOf(String.format(java.util.Locale.US, "%.1f", currentProfile.targetBodyTempMax)) }
    var pefBaselineInput by remember(currentProfile) { mutableStateOf(currentProfile.pefBaselineLpm.toInt().toString()) }
    var hasRespiratory by remember(currentProfile) { mutableStateOf(currentProfile.hasAsthmaOrCopd) }
    var savedMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "User Profile & Personal Baselines",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure clinician-recommended target thresholds and respiratory baseline values.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Demographic Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name / Identifier") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it },
                        label = { Text("Age") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Clinical Baseline Thresholds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text(text = "Resting Heart Rate Range (BPM)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = hrMinInput, onValueChange = { hrMinInput = it }, label = { Text("Min BPM") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = hrMaxInput, onValueChange = { hrMaxInput = it }, label = { Text("Max BPM") }, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "Normal Body Temperature Range (°C)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = tempMinInput, onValueChange = { tempMinInput = it }, label = { Text("Min °C") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = tempMaxInput, onValueChange = { tempMaxInput = it }, label = { Text("Max °C") }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Respiratory Monitoring Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Track Asthma / COPD Symptoms", fontSize = 13.sp)
                        Switch(checked = hasRespiratory, onCheckedChange = { hasRespiratory = it })
                    }

                    if (hasRespiratory) {
                        OutlinedTextField(
                            value = pefBaselineInput,
                            onValueChange = { pefBaselineInput = it },
                            label = { Text("Clinician Baseline PEF (L/min)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.saveProfile(
                        currentProfile.copy(
                            name = nameInput,
                            age = ageInput.toIntOrNull() ?: currentProfile.age,
                            targetHrRestingMin = hrMinInput.toDoubleOrNull() ?: currentProfile.targetHrRestingMin,
                            targetHrRestingMax = hrMaxInput.toDoubleOrNull() ?: currentProfile.targetHrRestingMax,
                            targetBodyTempMin = tempMinInput.toDoubleOrNull() ?: currentProfile.targetBodyTempMin,
                            targetBodyTempMax = tempMaxInput.toDoubleOrNull() ?: currentProfile.targetBodyTempMax,
                            pefBaselineLpm = pefBaselineInput.toDoubleOrNull() ?: currentProfile.pefBaselineLpm,
                            hasAsthmaOrCopd = hasRespiratory
                        )
                    )
                    savedMessage = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (savedMessage) "Settings Saved!" else "Save Profile Baselines", fontSize = 13.sp)
            }
        }
    }
}
