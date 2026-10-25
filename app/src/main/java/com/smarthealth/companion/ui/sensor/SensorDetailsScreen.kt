package com.smarthealth.companion.ui.sensor

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.theme.VitalAltitude
import com.smarthealth.companion.theme.VitalPressure
import com.smarthealth.companion.theme.VitalSensorTemp
import com.smarthealth.companion.ui.components.VitalMetricCard
import com.smarthealth.companion.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SensorDetailsScreen(viewModel: MainViewModel) {
    val latestMap by viewModel.latestMeasurements.collectAsState()
    val location by viewModel.lastKnownLocation.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val timeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

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
                    text = "BMP180 & Environmental Sensor Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Technical verification, barometric altitude calculation, and GPS telemetry context.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Vital Scientific Distinction Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Sensor Telemetry Segregation Standard", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. BMP180 atmospheric pressure (hPa) measures local ambient air pressure. It is NEVER treated as human blood pressure (mmHg).\n2. BMP180 internal sensor temperature measures onboard hardware electronics and is strictly segregated from human body temperature.\n3. Barometric altitude is computed using the international hypsometric formula and fluctuates with changing weather fronts.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Environmental Cards
        item {
            VitalMetricCard(
                measurement = latestMap[MeasurementType.ATMOSPHERIC_PRESSURE],
                title = "Atmospheric Pressure (BMP180)",
                defaultUnit = "hPa",
                accentColor = VitalPressure,
                idleMessage = "Reading BMP180 barometric sensor..."
            )
        }

        item {
            VitalMetricCard(
                measurement = latestMap[MeasurementType.SENSOR_TEMPERATURE],
                title = "BMP180 Internal Sensor Temperature",
                defaultUnit = "°C",
                accentColor = VitalSensorTemp,
                idleMessage = "Reading internal BMP180 sensor temperature..."
            )
        }

        item {
            VitalMetricCard(
                measurement = latestMap[MeasurementType.ESTIMATED_ALTITUDE],
                title = "Calculated Barometric Altitude",
                defaultUnit = "m",
                accentColor = VitalAltitude,
                idleMessage = "Computing barometric altitude..."
            )
        }

        // GPS Location Context Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "GPS Location Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Switch(
                            checked = profile.enableGpsLogging,
                            onCheckedChange = { viewModel.toggleGpsLogging(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!profile.enableGpsLogging) {
                        Text(
                            text = "GPS logging is disabled. Location coordinates are never recorded or stored without explicit user consent.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        if (location != null) {
                            Text(
                                text = "Coordinates: %.5f° N, %.5f° E".format(location!!.latitude, location!!.longitude),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Accuracy: ±%.1f m • Updated: ${timeFormat.format(Date(location!!.timestampMs))}".format(location!!.accuracyMeters),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Acquiring GPS fix with balanced power mode...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
