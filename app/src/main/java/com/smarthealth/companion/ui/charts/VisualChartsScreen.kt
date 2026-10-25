package com.smarthealth.companion.ui.charts

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.theme.VitalAltitude
import com.smarthealth.companion.theme.VitalBodyTemp
import com.smarthealth.companion.theme.VitalHeartRate
import com.smarthealth.companion.theme.VitalPressure
import com.smarthealth.companion.theme.VitalSensorTemp
import com.smarthealth.companion.theme.VitalSpO2
import com.smarthealth.companion.ui.components.ComparisonLineChart
import com.smarthealth.companion.ui.components.InteractiveLineChart
import com.smarthealth.companion.ui.viewmodel.MainViewModel

enum class ChartDisplayMode(val label: String) {
    LINE_WITH_MARKERS("Line with Markers"),
    COMPARISON_LINES("Line Chart for Comparison"),
    DUAL_VIEW("All Charts")
}

@Composable
fun VisualChartsScreen(viewModel: MainViewModel) {
    val chartMeasurements by viewModel.chartMeasurements.collectAsState()
    val triMetricMeasurements by viewModel.triMetricComparisonMeasurements.collectAsState()
    val selectedMetric by viewModel.selectedMetricType.collectAsState()
    val selectedDuration by viewModel.selectedDurationDays.collectAsState()
    val forecast by viewModel.currentForecastResult.collectAsState()
    val summary by viewModel.currentAnalyticsSummary.collectAsState()

    var displayMode by remember { mutableStateOf(ChartDisplayMode.DUAL_VIEW) }

    val durations = listOf(1, 3, 7, 14, 30)
    val metrics = listOf(
        MeasurementType.HEART_RATE,
        MeasurementType.BODY_TEMPERATURE,
        MeasurementType.ATMOSPHERIC_PRESSURE,
        MeasurementType.SENSOR_TEMPERATURE,
        MeasurementType.ESTIMATED_ALTITUDE,
        MeasurementType.SPO2
    )

    val metricColor = when (selectedMetric) {
        MeasurementType.HEART_RATE -> VitalHeartRate
        MeasurementType.BODY_TEMPERATURE -> VitalBodyTemp
        MeasurementType.ATMOSPHERIC_PRESSURE -> VitalPressure
        MeasurementType.SENSOR_TEMPERATURE -> VitalSensorTemp
        MeasurementType.ESTIMATED_ALTITUDE -> VitalAltitude
        MeasurementType.SPO2 -> VitalSpO2
        else -> TealPrimary
    }

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
                    text = "Visual Line Graph Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Line graphs with distinct vertex markers, Y-axis scales, and multi-signal comparison.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Chart View Mode Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartDisplayMode.values().forEach { mode ->
                    FilterChip(
                        selected = displayMode == mode,
                        onClick = { displayMode = mode },
                        label = { Text(text = mode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Duration selector
        item {
            Column {
                Text(
                    text = "Time Horizon",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }

        // Metric selector (shown for Line with Markers or Dual View)
        if (displayMode != ChartDisplayMode.COMPARISON_LINES) {
            item {
                Column {
                    Text(
                        text = "Active Signal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    selectedContainerColor = metricColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // 1. Line with Markers Card
        if (displayMode == ChartDisplayMode.LINE_WITH_MARKERS || displayMode == ChartDisplayMode.DUAL_VIEW) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Line with Markers: ${selectedMetric.displayName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Unit: ${selectedMetric.defaultUnit} • Evenly spaced nodes",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "${chartMeasurements.size} readings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = metricColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        InteractiveLineChart(
                            measurements = chartMeasurements,
                            lineColor = metricColor,
                            baselineMedian = summary?.baselineMedian,
                            predictedValue = if (forecast?.isReliable == true) forecast?.predictedValue else null,
                            uncertaintyLower = if (forecast?.isReliable == true) forecast?.uncertaintyLower else null,
                            uncertaintyUpper = if (forecast?.isReliable == true) forecast?.uncertaintyUpper else null
                        )
                    }
                }
            }
        }

        // 2. Line Chart for Comparison Card (Multi-Metric)
        if (displayMode == ChartDisplayMode.COMPARISON_LINES || displayMode == ChartDisplayMode.DUAL_VIEW) {
            item {
                ComparisonLineChart(triMetricData = triMetricMeasurements)
            }
        }

        // 3. Visual Legend & Guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Line Graph Legend & Guide",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(metricColor)
                                .border(1.dp, Color(0xFFE0F2FE), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Circular Node Markers: Measured sensor data points (touch to inspect)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .background(Color(0xFF2ED573))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Green Dashed Line: User personal baseline median",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF4757))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Red Node: Baseline outlier / unusual event",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .background(Color(0xFF8854D0))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Purple Dashed: 95% statistical forecast projection",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
