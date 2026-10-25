package com.smarthealth.companion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthealth.companion.model.BaselineComparison
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.theme.StatusAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class PlottedNode(
    val index: Int,
    val value: Double,
    val formattedValue: String,
    val unit: String,
    val timestampMs: Long,
    val isOutlier: Boolean = false,
    val sourceLabel: String = ""
)

/**
 * Modern Line Graph with Markers (matching WallStreetMojo style).
 * Features:
 * - Dedicated Y-axis numerical labels on the left (e.g. 120, 100, 80, 60, 40)
 * - Horizontal grid lines
 * - Solid polyline with smooth joins
 * - Circular markers (dots with outer border + contrasting inner fill) on EVERY data point
 * - Horizontal spacing prevents burst readings from stacking vertically
 * - Interactive tap tooltip inspecting point telemetry
 */
@Composable
fun InteractiveLineChart(
    measurements: List<Measurement>,
    lineColor: Color,
    baselineMedian: Double? = null,
    predictedValue: Double? = null,
    uncertaintyLower: Double? = null,
    uncertaintyUpper: Double? = null,
    modifier: Modifier = Modifier
) {
    var selectedNode by remember { mutableStateOf<PlottedNode?>(null) }
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    // Downsample/sample up to 20 evenly spaced representative chronological points
    // to ensure clean horizontal progression without vertical clustering
    val plottedNodes = remember(measurements) {
        val valid = measurements.filter { it.numericValue > 0 }.sortedBy { it.phoneReceiptTimestampMs }
        if (valid.isEmpty()) {
            emptyList()
        } else {
            val maxPoints = 20
            if (valid.size <= maxPoints) {
                valid.mapIndexed { i, m ->
                    PlottedNode(
                        index = i,
                        value = m.numericValue,
                        formattedValue = m.formattedValue(),
                        unit = m.unit,
                        timestampMs = m.phoneReceiptTimestampMs,
                        isOutlier = m.baselineComparison == BaselineComparison.UNUSUAL_CHANGE ||
                                m.baselineComparison == BaselineComparison.URGENT_CONCERN,
                        sourceLabel = m.source.label
                    )
                }
            } else {
                // Pick maxPoints evenly distributed across the collection
                (0 until maxPoints).map { step ->
                    val rawIndex = ((step.toDouble() / (maxPoints - 1)) * (valid.size - 1)).roundToInt()
                    val m = valid[rawIndex.coerceIn(0, valid.size - 1)]
                    PlottedNode(
                        index = step,
                        value = m.numericValue,
                        formattedValue = m.formattedValue(),
                        unit = m.unit,
                        timestampMs = m.phoneReceiptTimestampMs,
                        isOutlier = m.baselineComparison == BaselineComparison.UNUSUAL_CHANGE ||
                                m.baselineComparison == BaselineComparison.URGENT_CONCERN,
                        sourceLabel = m.source.label
                    )
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Interactive Tooltip when user touches a node
        if (selectedNode != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        lineColor.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(lineColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${dateFormat.format(Date(selectedNode!!.timestampMs))}: ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${selectedNode!!.formattedValue} ${selectedNode!!.unit}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = lineColor
                        )
                    }
                    if (selectedNode!!.isOutlier) {
                        Text(
                            text = "⚠️ Outlier",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusAlert
                        )
                    }
                }
            }
        }

        if (plottedNodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recorded readings for selected interval",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else {
            val minVal = remember(plottedNodes) {
                val m = plottedNodes.minOf { it.value }
                (m * 0.95).coerceAtLeast(0.0)
            }
            val maxVal = remember(plottedNodes) {
                val m = plottedNodes.maxOf { it.value }
                (m * 1.05).coerceAtLeast(minVal + 1.0)
            }
            val valRange = (maxVal - minVal).coerceAtLeast(1.0)

            // 5 Y-axis tick values (Top to Bottom)
            val yTicks = remember(minVal, maxVal) {
                listOf(
                    maxVal,
                    minVal + (valRange * 0.75),
                    minVal + (valRange * 0.50),
                    minVal + (valRange * 0.25),
                    minVal
                )
            }

            val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

            // Main Chart with Left Y-Axis labels and Canvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(top = 12.dp, bottom = 12.dp, start = 8.dp, end = 12.dp)
            ) {
                // 1. Y-Axis Numerical Labels (Left Column)
                Column(
                    modifier = Modifier
                        .width(42.dp)
                        .height(196.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    yTicks.forEach { tick ->
                        Text(
                            text = if (tick >= 100) "%.0f".format(tick) else "%.1f".format(tick),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 2. Chart Canvas (Grid + Polyline + Markers)
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(196.dp)
                        .pointerInput(plottedNodes) {
                            detectTapGestures { offset ->
                                val w = size.width
                                val n = plottedNodes.size
                                if (n > 0) {
                                    val stepX = if (n > 1) w / (n - 1) else w
                                    val tappedIndex = ((offset.x / stepX).roundToInt()).coerceIn(0, n - 1)
                                    selectedNode = plottedNodes[tappedIndex]
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // A. Draw Horizontal Grid Lines
                    for (i in 0..4) {
                        val y = h * (i / 4f)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        )
                    }

                    // B. Draw Baseline Reference Line if available
                    if (baselineMedian != null && baselineMedian in minVal..maxVal) {
                        val baselineY = h - (((baselineMedian - minVal) / valRange) * h).toFloat()
                        drawLine(
                            color = Color(0xFF2ED573).copy(alpha = 0.7f),
                            start = Offset(0f, baselineY),
                            end = Offset(w, baselineY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                        )
                    }

                    // C. Construct Line Path across evenly distributed X coordinates
                    val path = Path()
                    val n = plottedNodes.size
                    val points = mutableListOf<Offset>()

                    for (i in plottedNodes.indices) {
                        val x = if (n > 1) (i.toFloat() / (n - 1).toFloat()) * w else (w / 2f)
                        val y = h - (((plottedNodes[i].value - minVal) / valRange) * h).toFloat()
                        val pt = Offset(x, y)
                        points.add(pt)

                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    // Draw the Connecting Line
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // D. Draw Markers on EVERY Vertex (matching "Line with Markers")
                    val markerOuterRadius = 4.5.dp.toPx()
                    val markerInnerRadius = 2.2.dp.toPx()

                    for (i in points.indices) {
                        val pt = points[i]
                        val node = plottedNodes[i]
                        val isSelected = selectedNode?.index == node.index

                        // Outer glowing halo if selected
                        if (isSelected) {
                            drawCircle(
                                color = lineColor.copy(alpha = 0.35f),
                                radius = 9.dp.toPx(),
                                center = pt
                            )
                        }

                        // Node Outer Circle (Colored border matching line)
                        val nodeBorderColor = if (node.isOutlier) StatusAlert else lineColor
                        drawCircle(
                            color = nodeBorderColor,
                            radius = markerOuterRadius,
                            center = pt
                        )

                        // Node Inner Fill (Contrasting cyan/white fill like WallStreetMojo reference)
                        val nodeInnerColor = if (node.isOutlier) Color(0xFFFFD2D2) else Color(0xFFE0F2FE)
                        drawCircle(
                            color = nodeInnerColor,
                            radius = markerInnerRadius,
                            center = pt
                        )
                    }

                    // E. Forecast Projection if available
                    if (predictedValue != null && points.isNotEmpty()) {
                        val lastPt = points.last()
                        val predY = h - (((predictedValue - minVal) / valRange) * h).toFloat()
                        val predX = w

                        drawLine(
                            color = Color(0xFF8854D0),
                            start = lastPt,
                            end = Offset(predX, predY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        )

                        // Projected Marker
                        drawCircle(
                            color = Color(0xFF8854D0),
                            radius = 5.dp.toPx(),
                            center = Offset(predX, predY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(predX, predY)
                        )
                    }
                }
            }

            // 3. X-Axis Time / Date Range Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 48.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start: ${dateFormat.format(Date(plottedNodes.first().timestampMs))}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Latest: ${dateFormat.format(Date(plottedNodes.last().timestampMs))}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Line Chart for Comparison (matching WallStreetMojo example 3 & 4).
 * Simultaneously plots Heart Rate, Atmospheric Pressure, and Body Temperature
 * on a normalized 0% - 100% relative baseline scale with distinct colored lines & markers.
 */
@Composable
fun ComparisonLineChart(
    triMetricData: Map<MeasurementType, List<Measurement>>,
    modifier: Modifier = Modifier
) {
    val hrList = triMetricData[MeasurementType.HEART_RATE]?.filter { it.numericValue > 0 } ?: emptyList()
    val pressureList = triMetricData[MeasurementType.ATMOSPHERIC_PRESSURE]?.filter { it.numericValue > 0 } ?: emptyList()
    val tempList = triMetricData[MeasurementType.BODY_TEMPERATURE]?.filter { it.numericValue > 0 } ?: emptyList()

    val maxPoints = 16
    val hasData = hrList.isNotEmpty() || pressureList.isNotEmpty() || tempList.isNotEmpty()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Line Chart for Multi-Signal Comparison",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Color-Coded Comparison Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ComparisonLegendItem(label = "Heart Rate", color = Color(0xFFFF4757), markerFill = Color(0xFFE0F2FE))
                ComparisonLegendItem(label = "Pressure", color = Color(0xFF00A8FF), markerFill = Color.White)
                ComparisonLegendItem(label = "Temperature", color = Color(0xFFFFA502), markerFill = Color(0xFFFEF3C7))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Awaiting multi-metric hardware data", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = 8.dp, bottom = 8.dp)
                ) {
                    // Y-Axis (0% - 100% relative baseline scale)
                    Column(
                        modifier = Modifier
                            .width(38.dp)
                            .height(184.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        listOf("100%", "75%", "50%", "25%", "0%").forEach { tick ->
                            Text(
                                text = tick,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Canvas(modifier = Modifier.weight(1f).height(184.dp)) {
                        val w = size.width
                        val h = size.height

                        // Horizontal grid lines
                        for (i in 0..4) {
                            val y = h * (i / 4f)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )
                        }

                        // Helper to plot a series
                        fun drawSeries(
                            list: List<Measurement>,
                            lineCol: Color,
                            markerInnerCol: Color
                        ) {
                            if (list.isEmpty()) return
                            val sorted = list.sortedBy { it.phoneReceiptTimestampMs }
                            val sampled = if (sorted.size <= maxPoints) sorted
                            else (0 until maxPoints).map { s ->
                                val idx = ((s.toDouble() / (maxPoints - 1)) * (sorted.size - 1)).roundToInt()
                                sorted[idx.coerceIn(0, sorted.size - 1)]
                            }

                            val minV = sampled.minOf { it.numericValue }
                            val maxV = sampled.maxOf { it.numericValue }
                            val range = (maxV - minV).coerceAtLeast(1.0)

                            val pts = mutableListOf<Offset>()
                            val pth = Path()

                            for (i in sampled.indices) {
                                val x = if (sampled.size > 1) (i.toFloat() / (sampled.size - 1).toFloat()) * w else (w / 2f)
                                val norm = ((sampled[i].numericValue - minV) / range).toFloat()
                                val y = h - (norm * (h * 0.82f)) - (h * 0.08f)
                                val pt = Offset(x, y)
                                pts.add(pt)
                                if (i == 0) pth.moveTo(x, y) else pth.lineTo(x, y)
                            }

                            drawPath(
                                path = pth,
                                color = lineCol,
                                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // Markers
                            for (pt in pts) {
                                drawCircle(color = lineCol, radius = 4.dp.toPx(), center = pt)
                                drawCircle(color = markerInnerCol, radius = 2.dp.toPx(), center = pt)
                            }
                        }

                        // 1. Plot Heart Rate
                        drawSeries(hrList, Color(0xFFFF4757), Color(0xFFE0F2FE))
                        // 2. Plot Atmospheric Pressure
                        drawSeries(pressureList, Color(0xFF00A8FF), Color.White)
                        // 3. Plot Body Temperature
                        drawSeries(tempList, Color(0xFFFFA502), Color(0xFFFEF3C7))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, top = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Window Start", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Real-Time Telemetry", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ComparisonLegendItem(label: String, color: Color, markerFill: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(markerFill)
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
