package com.smarthealth.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthealth.companion.model.BaselineComparison
import com.smarthealth.companion.model.DataSource
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.theme.SourceSimulated
import com.smarthealth.companion.theme.SourceWearable
import com.smarthealth.companion.theme.StatusAlert
import com.smarthealth.companion.theme.StatusNormal
import com.smarthealth.companion.theme.StatusWarning

@Composable
fun VitalMetricCard(
    measurement: Measurement?,
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    defaultUnit: String = "",
    idleMessage: String = "Waiting for sensor data...",
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (measurement != null) {
                    SourceBadge(source = measurement.source)
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Standby",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (measurement != null) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = measurement.formattedValue(),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = measurement.unit,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BaselineBadge(comparison = measurement.baselineComparison)

                    Text(
                        text = "Quality: ${measurement.dataQualityStatus.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "--",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    if (defaultUnit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = defaultUnit,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = idleMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SourceBadge(source: DataSource) {
    val (bgColor, textColor, label) = when (source) {
        DataSource.WEARABLE -> Triple(SourceWearable.copy(alpha = 0.15f), SourceWearable, "S3 Wearable")
        DataSource.SIMULATED -> Triple(SourceSimulated.copy(alpha = 0.2f), SourceSimulated, "Simulated Demo")
        DataSource.MANUAL -> Triple(Color(0xFF2196F3).copy(alpha = 0.15f), Color(0xFF2196F3), "Manual")
        DataSource.IMPORTED -> Triple(Color(0xFF9C27B0).copy(alpha = 0.15f), Color(0xFF9C27B0), "Imported")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun BaselineBadge(comparison: BaselineComparison) {
    val (color, text) = when (comparison) {
        BaselineComparison.WITHIN_BASELINE -> Pair(StatusNormal, "Baseline Normal")
        BaselineComparison.UNUSUAL_CHANGE -> Pair(StatusWarning, "Unusual Shift")
        BaselineComparison.REVIEW_QUALITY -> Pair(Color(0xFF747D8C), "Check Sensor")
        BaselineComparison.INSUFFICIENT_DATA -> Pair(Color(0xFF747D8C), "Learning Baseline")
        BaselineComparison.URGENT_CONCERN -> Pair(StatusAlert, "Urgent Notice")
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
