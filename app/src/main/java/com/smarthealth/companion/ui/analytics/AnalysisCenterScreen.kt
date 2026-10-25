package com.smarthealth.companion.ui.analytics

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthealth.companion.model.AnalyticsSummary
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.model.TriMetricEnvironmentAnalysis
import com.smarthealth.companion.theme.StatusAlert
import com.smarthealth.companion.theme.StatusNormal
import com.smarthealth.companion.theme.StatusWarning
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalysisCenterScreen(viewModel: MainViewModel) {
    val selectedDuration by viewModel.selectedDurationDays.collectAsState()
    val selectedMetric by viewModel.selectedMetricType.collectAsState()
    val summary by viewModel.currentAnalyticsSummary.collectAsState()
    val triMetric by viewModel.triMetricAnalysis.collectAsState()
    val aiSummary by viewModel.aiSummaryText.collectAsState()

    val durations = listOf(1, 3, 7, 14, 21, 28, 30)
    val metrics = listOf(
        MeasurementType.HEART_RATE,
        MeasurementType.BODY_TEMPERATURE,
        MeasurementType.ATMOSPHERIC_PRESSURE,
        MeasurementType.SENSOR_TEMPERATURE,
        MeasurementType.ESTIMATED_ALTITUDE,
        MeasurementType.SPO2
    )

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

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
                    text = "Historical Analysis Center",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select historical window to compute descriptive statistics, baseline shifts & trends.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Duration Selector Chips (1, 3, 7, 14, 21, 28, 30 days)
        item {
            Text(
                text = "Analysis Duration Horizon",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Tri-Parametric Space & Body Analysis (Room Pressure, Heart Rate, Temperature)
        item {
            TriMetricEnvironmentCard(
                triMetric = triMetric,
                onTriggerTestAlert = { viewModel.triggerSimulatedAbnormalAlert() }
            )
        }

        // Metric Selector Chips
        item {
            Text(
                text = "Target Health & Environmental Metric",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metrics.forEach { m ->
                    FilterChip(
                        selected = selectedMetric == m,
                        onClick = { viewModel.selectMetric(m) },
                        label = { Text(text = m.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Verified Date Range Info Card
        if (summary != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Actual Analyzed Range: ${dateFormat.format(Date(summary!!.startDateMs))} to ${dateFormat.format(Date(summary!!.endDateMs))}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Data Completeness: %.1f%% (%d valid / %d total observations)".format(
                                    summary!!.dataCompletenessPercent,
                                    summary!!.validSamples,
                                    summary!!.totalRecordedSamples
                                ),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Statistical Metrics 2x2 Grid
            item {
                Text(
                    text = "Computed Statistical Parameters (${summary!!.measurementType.displayName})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                StatisticsGrid(summary = summary!!)
            }
        }

        // AI Health Summary Narrative Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Summary",
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Health & Telemetry Narrative",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (aiSummary.isNotEmpty()) aiSummary else "Loading narrative summary...",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticsGrid(summary: AnalyticsSummary) {
    val unit = summary.measurementType.defaultUnit

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBox(title = "Average (Mean)", value = "%.1f %s".format(summary.average, unit), modifier = Modifier.weight(1f))
            StatBox(title = "Median (P50)", value = "%.1f %s".format(summary.median, unit), modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBox(title = "Min - Max Range", value = "%.1f - %.1f".format(summary.min, summary.max), modifier = Modifier.weight(1f))
            StatBox(title = "Std Deviation", value = "±%.2f %s".format(summary.standardDeviation, unit), modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBox(title = "IQR (P25 - P75)", value = "%.1f (%.1f - %.1f)".format(summary.iqr, summary.percentile25, summary.percentile75), modifier = Modifier.weight(1f))
            StatBox(title = "Trend Direction", value = summary.trendDirection.label, modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBox(title = "Rate of Change", value = "%.3f %s/hr".format(summary.rateOfChangePerHour, unit), modifier = Modifier.weight(1f))
            StatBox(title = "Unusual Readings", value = "${summary.unusualMeasurementCount} events (~${summary.unusualDurationMinutes}m)", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatBox(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun TriMetricEnvironmentCard(
    triMetric: TriMetricEnvironmentAnalysis?,
    onTriggerTestAlert: () -> Unit
) {
    if (triMetric == null) return

    val isAbnormal = triMetric.isOverallAbnormal
    val statusColor = when {
        !triMetric.hasPressureData && !triMetric.hasHeartRateData && !triMetric.hasTemperatureData -> Color(0xFF747D8C)
        isAbnormal -> StatusAlert
        else -> StatusNormal
    }

    val cardBg = if (isAbnormal) Color(0xFFFFF5F5) else MaterialTheme.colorScheme.surface
    val titleColor = if (isAbnormal) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurface
    val subColor = if (isAbnormal) Color(0xFF7F1D1D) else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isAbnormal) 1.5.dp else 1.dp,
                color = if (isAbnormal) Color(0xFFEF4444) else TealPrimary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAbnormal) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tri-Parametric Space & Body Analysis",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    Text(
                        text = "Correlating Room Pressure + Heart Rate + Temperature (${triMetric.durationLabel})",
                        fontSize = 11.sp,
                        color = subColor
                    )
                }
            }

            // Overall Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = triMetric.overallLevel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            // 3-Metric Metric Comparison Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Room Pressure (BMP180)
                TriMetricSubCard(
                    title = "Atmosphere",
                    sub = "BMP180 Room",
                    value = if (triMetric.hasPressureData) "%.1f hPa".format(triMetric.pressureMean) else "--",
                    range = if (triMetric.hasPressureData) "%.0f-%.0f".format(triMetric.pressureMin, triMetric.pressureMax) else "No data",
                    isAbnormal = triMetric.pressureIsAbnormal,
                    modifier = Modifier.weight(1f)
                )

                // Heart Rate (MAX30102)
                TriMetricSubCard(
                    title = "Heart Rate",
                    sub = "MAX30102 Body",
                    value = if (triMetric.hasHeartRateData) "%.0f BPM".format(triMetric.heartRateMean) else "--",
                    range = if (triMetric.hasHeartRateData) "%.0f-%.0f".format(triMetric.heartRateMin, triMetric.heartRateMax) else "No data",
                    isAbnormal = triMetric.heartRateIsAbnormal,
                    modifier = Modifier.weight(1f)
                )

                // Body Temp (DS18B20)
                TriMetricSubCard(
                    title = "Body Temp",
                    sub = "DS18B20 Skin",
                    value = if (triMetric.hasTemperatureData) "%.1f °C".format(triMetric.temperatureMean) else "--",
                    range = if (triMetric.hasTemperatureData) "%.1f-%.1f".format(triMetric.temperatureMin, triMetric.temperatureMax) else "No data",
                    isAbnormal = triMetric.temperatureIsAbnormal,
                    modifier = Modifier.weight(1f)
                )
            }

            // Clinical / Relocation Guidance Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isAbnormal) Color(0xFFFEE2E2)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .border(
                        1.dp,
                        if (isAbnormal) Color(0xFFEF4444)
                        else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = if (isAbnormal) "⚠️ ACTIONABLE RELOCATION GUIDANCE" else "CLINICAL GUIDANCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAbnormal) Color(0xFFB91C1C) else TealPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = triMetric.clinicalGuidance,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 17.sp,
                        color = if (isAbnormal) Color(0xFF7F1D1D) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Analytical Narrative
            if (triMetric.detailedNarrative.isNotEmpty()) {
                Text(
                    text = triMetric.detailedNarrative,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = if (isAbnormal) Color(0xFF450A0A) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TriMetricSubCard(
    title: String,
    sub: String,
    value: String,
    range: String,
    isAbnormal: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isAbnormal) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val containerBg = if (isAbnormal) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.surface
    val valueColor = if (isAbnormal) Color(0xFFDC2626) else TealPrimary

    Card(
        modifier = modifier.border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isAbnormal) Color(0xFF7F1D1D) else MaterialTheme.colorScheme.onSurface)
            Text(text = sub, fontSize = 9.sp, color = if (isAbnormal) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = range, fontSize = 9.sp, color = if (isAbnormal) Color(0xFF7F1D1D) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
