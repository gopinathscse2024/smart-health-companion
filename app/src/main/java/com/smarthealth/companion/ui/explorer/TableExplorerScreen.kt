package com.smarthealth.companion.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthealth.companion.model.BaselineComparison
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.ui.components.BaselineBadge
import com.smarthealth.companion.ui.components.SourceBadge
import com.smarthealth.companion.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TableExplorerScreen(viewModel: MainViewModel) {
    val allMeasurements by viewModel.allExplorerMeasurements.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<MeasurementType?>(null) }
    var onlyAnomaliesFilter by remember { mutableStateOf(false) }
    var selectedDetailRecord by remember { mutableStateOf<Measurement?>(null) }

    val filteredMeasurements = remember(allMeasurements, searchQuery, selectedTypeFilter, onlyAnomaliesFilter) {
        allMeasurements.filter { m ->
            val matchesSearch = searchQuery.isEmpty() ||
                    m.type.displayName.contains(searchQuery, ignoreCase = true) ||
                    m.formattedValue().contains(searchQuery) ||
                    (m.deviceId?.contains(searchQuery, ignoreCase = true) == true)
            val matchesType = selectedTypeFilter == null || m.type == selectedTypeFilter
            val matchesAnomaly = !onlyAnomaliesFilter || (m.baselineComparison == BaselineComparison.UNUSUAL_CHANGE || m.baselineComparison == BaselineComparison.URGENT_CONCERN)
            matchesSearch && matchesType && matchesAnomaly
        }
    }

    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Telemetry Data Explorer & Audit Table",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Inspect raw observations, provenance timestamps, signal quality, and baseline classifications.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by value, metric, or device ID...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Quick Filters Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == null && !onlyAnomaliesFilter,
                    onClick = {
                        selectedTypeFilter = null
                        onlyAnomaliesFilter = false
                    },
                    label = { Text("All (${allMeasurements.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                )

                FilterChip(
                    selected = onlyAnomaliesFilter,
                    onClick = { onlyAnomaliesFilter = !onlyAnomaliesFilter },
                    label = { Text("Unusual Outliers Only", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFF4757), selectedLabelColor = Color.White)
                )

                MeasurementType.values().take(4).forEach { type ->
                    FilterChip(
                        selected = selectedTypeFilter == type,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == type) null else type },
                        label = { Text(type.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                    )
                }
            }
        }

        // Table Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Date / Time", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.3f))
                    Text(text = "Metric", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
                    Text(text = "Reading", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f))
                    Text(text = "Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.4f))
                }
            }
        }

        // Data Rows
        if (filteredMeasurements.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No measurements matched the selected filters.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filteredMeasurements.take(150)) { item ->
                TableRowItem(
                    measurement = item,
                    dateFormat = dateFormat,
                    onClick = { selectedDetailRecord = item }
                )
            }
        }
    }

    // Record Detail Modal
    if (selectedDetailRecord != null) {
        RecordDetailDialog(
            measurement = selectedDetailRecord!!,
            onDismiss = { selectedDetailRecord = null }
        )
    }
}

@Composable
fun TableRowItem(
    measurement: Measurement,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateFormat.format(Date(measurement.phoneReceiptTimestampMs)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.3f)
            )

            Text(
                text = measurement.type.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.5f)
            )

            Text(
                text = "${measurement.formattedValue()} ${measurement.unit}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.2f)
            )

            Box(modifier = Modifier.weight(1.4f)) {
                BaselineBadge(comparison = measurement.baselineComparison)
            }
        }
    }
}

@Composable
fun RecordDetailDialog(
    measurement: Measurement,
    onDismiss: () -> Unit
) {
    val fullDate = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "${measurement.type.displayName} Telemetry Record", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow("Measurement ID", measurement.id)
                DetailRow("Timestamp", fullDate.format(Date(measurement.phoneReceiptTimestampMs)))
                DetailRow("Primary Reading", "${measurement.formattedValue()} ${measurement.unit}")
                if (measurement.secondaryValue != null) {
                    DetailRow("Secondary Reading", "${measurement.secondaryValue} ${measurement.unit}")
                }
                DetailRow("Data Source", measurement.source.label)
                DetailRow("Device ID", measurement.deviceId ?: "N/A")
                DetailRow("Signal Quality", "${measurement.signalQuality}%")
                DetailRow("Quality Status", measurement.dataQualityStatus.label)
                DetailRow("Baseline Status", measurement.baselineComparison.label)
                if (measurement.latitude != null) {
                    DetailRow("GPS Coordinates", "%.5f, %.5f (±%.1fm)".format(measurement.latitude, measurement.longitude, measurement.locationAccuracy ?: 0f))
                } else {
                    DetailRow("Location", "No GPS Associated (User Privacy Protected)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TealPrimary)
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
