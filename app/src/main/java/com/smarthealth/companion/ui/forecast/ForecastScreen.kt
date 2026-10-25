package com.smarthealth.companion.ui.forecast

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthealth.companion.model.ForecastResult
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.theme.PredictionBand
import com.smarthealth.companion.theme.PredictionColor
import com.smarthealth.companion.theme.StatusAlert
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.ui.viewmodel.MainViewModel

@Composable
fun ForecastScreen(viewModel: MainViewModel) {
    val selectedMetric by viewModel.selectedMetricType.collectAsState()
    val selectedHorizon by viewModel.selectedForecastHorizonHours.collectAsState()
    val forecast by viewModel.currentForecastResult.collectAsState()

    val horizons = listOf(
        Pair(6, "6 Hours"),
        Pair(24, "24 Hours"),
        Pair(72, "3 Days"),
        Pair(168, "7 Days")
    )

    val metrics = listOf(
        MeasurementType.HEART_RATE,
        MeasurementType.BODY_TEMPERATURE,
        MeasurementType.ATMOSPHERIC_PRESSURE,
        MeasurementType.SPO2
    )

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
                    text = "Predictive Analytics & Forecasting",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "On-device explainable trend projections with 95% statistical confidence intervals.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Horizon Selector Chips
        item {
            Text(
                text = "Forecast Horizon Window",
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
                horizons.forEach { (h, label) ->
                    FilterChip(
                        selected = selectedHorizon == h,
                        onClick = { viewModel.selectForecastHorizon(h) },
                        label = { Text(text = label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PredictionColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Target Metric Selector
        item {
            Text(
                text = "Target Forecast Signal",
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

        // Forecast Result Presentation
        if (forecast != null) {
            item {
                ForecastResultCard(result = forecast!!)
            }
        }
    }
}

@Composable
fun ForecastResultCard(result: ForecastResult) {
    val unit = result.measurementType.defaultUnit

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PredictionColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Timeline, contentDescription = null, tint = PredictionColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${result.measurementType.displayName} Projection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PredictionBand)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = result.horizonLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PredictionColor)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!result.isReliable) {
                // Insufficient data notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StatusAlert.copy(alpha = 0.12f))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusAlert, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Insufficient Data",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = StatusAlert
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Not enough data to make a reliable prediction (${result.sampleCountUsed}/15 required observations). Collect additional sensor readings or activate demo mode.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                // Predicted Value & Uncertainty Band
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "%.1f".format(result.predictedValue),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = PredictionColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = unit,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "95% Uncertainty Band", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "[%.1f - %.1f %s]".format(result.uncertaintyLower, result.uncertaintyUpper, unit),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Projected Trend: ${result.trendDescription}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Contributing Factors
                Text(text = "Contributing Statistical Factors:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                result.contributingFactors.forEach { factor ->
                    Text(text = "• $factor", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Non-Diagnostic Disclaimer Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = result.disclaimer,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
