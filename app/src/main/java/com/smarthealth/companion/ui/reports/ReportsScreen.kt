package com.smarthealth.companion.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.ui.viewmodel.MainViewModel

@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedDuration by viewModel.selectedDurationDays.collectAsState()
    val exportStatus by viewModel.exportStatusMessage.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val summary by viewModel.currentAnalyticsSummary.collectAsState()

    val durations = listOf(1, 3, 7, 14, 21, 28, 30)

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
                    text = "Clinical Reports & Data Export",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Generate professional multi-page PDF reports and RFC 4180 CSV exports for clinician consultations.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Duration Horizon Chips
        item {
            Text(text = "Select Report Timeframe", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durations.forEach { d ->
                    val label = if (d == 1) "24 Hours" else "$d Days"
                    FilterChip(
                        selected = selectedDuration == d,
                        onClick = { viewModel.selectDuration(d) },
                        label = { Text(text = label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                    )
                }
            }
        }

        // Export Actions Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Export Report Options ($selectedDuration Days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.exportAndSharePdf(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Export & Share Clinical PDF Report", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.exportAndShareCsv(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Export & Share Telemetry CSV Dataset", fontSize = 13.sp)
                    }

                    if (exportStatus != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TealPrimary.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Text(text = exportStatus!!, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                        }
                    }
                }
            }
        }

        // Report Preview Blueprint
        item {
            Text(
                text = "Report Structure & Contents",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ReportSectionItem("1. Patient / Profile Metadata", "Name: ${profile.name}, Age: ${profile.age}, Respiratory Track: ${if (profile.hasAsthmaOrCopd) "Active" else "None"}")
                    ReportSectionItem("2. Device Telemetry Provenance", "ESP32-S3 Wearable (6e400001-...), BMP180 barometric sensor, GPS context (if permitted)")
                    ReportSectionItem("3. Computed Parametric Summary", "Mean, median, MAD, min/max, standard deviation, P25/P75 percentiles, completeness %")
                    ReportSectionItem("4. Baseline Anomaly & Outlier Triage", "Identifies statistically significant deviations from personal historical median")
                    ReportSectionItem("5. Respiratory & Symptom Chronology", "Peak Expiratory Flow (PEF) records, action plan zones, rescue medication counters")
                    ReportSectionItem("6. Explicit Clinical Disclaimer", "Clearly states report is non-diagnostic and prompts immediate care for emergency signs")
                }
            }
        }
    }
}

@Composable
fun ReportSectionItem(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
