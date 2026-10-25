package com.smarthealth.companion.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.smarthealth.companion.model.BleConnectionState
import com.smarthealth.companion.model.DataSource
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.theme.SourceSimulated
import com.smarthealth.companion.theme.StatusAlert
import com.smarthealth.companion.theme.StatusNormal
import com.smarthealth.companion.theme.StatusWarning
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.theme.VitalBodyTemp
import com.smarthealth.companion.theme.VitalHeartRate
import com.smarthealth.companion.theme.VitalPressure
import com.smarthealth.companion.ui.components.VitalMetricCard
import com.smarthealth.companion.ui.navigation.Screen
import com.smarthealth.companion.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val connectionState by viewModel.bleConnectionState.collectAsState()
    val lastSyncTime by viewModel.lastSyncTimestampMs.collectAsState()
    val latestMap by viewModel.latestMeasurements.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val timeFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy • HH:mm", Locale.getDefault()) }
    val syncFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Date & Time Header
        item {
            Column {
                Text(
                    text = "Smart Health Companion AI",
                    style = MaterialTheme.typography.titleLarge,
                    color = TealPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormat.format(Date()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Hardware Connection & Sync Status Banner
        item {
            BleStatusCard(
                state = connectionState,
                lastSyncMs = lastSyncTime,
                onConnectClick = { onNavigate(Screen.Ble.route) },
                onStartClick = { viewModel.sendBleStart() },
                onStopClick = { viewModel.sendBleStop() },
                onReadClick = { viewModel.sendBleRead() },
                onSyncClick = { viewModel.sendBleSyncTime() }
            )
        }

        // 4. Live Core Vital Cards
        item {
            Text(
                text = "Live Vital & Sensor Stream",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            val hr = latestMap[MeasurementType.HEART_RATE]
            // If wearable heart rate is older than 12 seconds, finger is not on sensor (matching OLED '--')
            val isHrFresh = hr != null && (hr.source != DataSource.WEARABLE || (System.currentTimeMillis() - hr.phoneReceiptTimestampMs < 12_000L))
            VitalMetricCard(
                measurement = if (isHrFresh) hr else null,
                title = "Optical Heart Rate",
                defaultUnit = "BPM",
                accentColor = VitalHeartRate,
                idleMessage = if (connectionState == BleConnectionState.NOTIFICATION_READY || connectionState == BleConnectionState.CONNECTED)
                    "Place finger on MAX30102 sensor"
                else
                    "Wearable disconnected • Sensor standby",
                onClick = {
                    viewModel.selectMetric(MeasurementType.HEART_RATE)
                    onNavigate(Screen.Charts.route)
                }
            )
        }

        item {
            val temp = latestMap[MeasurementType.BODY_TEMPERATURE]
            VitalMetricCard(
                measurement = temp,
                title = "Body Temperature",
                defaultUnit = "°C",
                accentColor = VitalBodyTemp,
                idleMessage = if (connectionState == BleConnectionState.NOTIFICATION_READY || connectionState == BleConnectionState.CONNECTED)
                    "Waiting for DS18B20 temperature reading..."
                else
                    "Wearable disconnected • Sensor standby",
                onClick = {
                    viewModel.selectMetric(MeasurementType.BODY_TEMPERATURE)
                    onNavigate(Screen.Charts.route)
                }
            )
        }

        item {
            val press = latestMap[MeasurementType.ATMOSPHERIC_PRESSURE]
            VitalMetricCard(
                measurement = press,
                title = "BMP180 Atmospheric Pressure",
                defaultUnit = "hPa",
                accentColor = VitalPressure,
                idleMessage = if (connectionState == BleConnectionState.NOTIFICATION_READY || connectionState == BleConnectionState.CONNECTED)
                    "Reading BMP180 barometric sensor..."
                else
                    "Wearable disconnected • Sensor standby",
                onClick = {
                    viewModel.selectMetric(MeasurementType.ATMOSPHERIC_PRESSURE)
                    onNavigate(Screen.SensorDetails.route)
                }
            )
        }

        // 5. Quick Action Hub
        item {
            Text(
                text = "Quick Clinical Actions",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigate(Screen.Ble.route) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Connect Wearable",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { onNavigate(Screen.Analysis.route) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Analyze Data", fontSize = 12.sp)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigate(Screen.Respiratory.route) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Log Symptoms", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onNavigate(Screen.Reports.route) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "View Reports", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun BleStatusCard(
    state: BleConnectionState,
    lastSyncMs: Long?,
    onConnectClick: () -> Unit,
    onStartClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onReadClick: () -> Unit = {},
    onSyncClick: () -> Unit = {}
) {
    val (statusColor, statusTitle) = when (state) {
        BleConnectionState.NOTIFICATION_READY, BleConnectionState.CONNECTED -> Pair(StatusNormal, "ESP32-S3 Connected")
        BleConnectionState.CONNECTING, BleConnectionState.DISCOVERING_SERVICES -> Pair(StatusWarning, "Connecting to S3...")
        BleConnectionState.SCANNING -> Pair(StatusWarning, "Scanning Nearby BLE...")
        BleConnectionState.RECONNECTING -> Pair(StatusWarning, "Reconnecting...")
        BleConnectionState.DISCONNECTED -> Pair(Color(0xFF747D8C), "Wearable Disconnected")
        BleConnectionState.ERROR -> Pair(StatusAlert, "BLE Comm Error")
    }

    val syncFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val isConnected = state == BleConnectionState.NOTIFICATION_READY || state == BleConnectionState.CONNECTED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConnectClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = statusTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val syncText = if (lastSyncMs != null) {
                            "Last packet received: ${syncFormat.format(Date(lastSyncMs))}"
                        } else "No telemetry received yet"
                        Text(
                            text = syncText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Manage BLE",
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isConnected) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onStartClick,
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = TealPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Start", fontSize = 11.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onStopClick,
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusAlert)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Stop", fontSize = 11.sp, color = StatusAlert, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onReadClick,
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Read", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onSyncClick,
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF6366F1))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Sync", fontSize = 11.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
