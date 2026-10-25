package com.smarthealth.companion.ui.ble

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthealth.companion.data.ble.BleManager
import com.smarthealth.companion.model.BleCommandLogEntry
import com.smarthealth.companion.model.BleConnectionState
import com.smarthealth.companion.model.BleDeviceItem
import com.smarthealth.companion.theme.SourceSimulated
import com.smarthealth.companion.theme.StatusAlert
import com.smarthealth.companion.theme.StatusNormal
import com.smarthealth.companion.theme.StatusWarning
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BleConnectionScreen(viewModel: MainViewModel) {
    val connectionState by viewModel.bleConnectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val rawPayload by viewModel.rawBlePayload.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val exportMessage by viewModel.exportStatusMessage.collectAsState()
    val commandLogs by viewModel.bleCommandLogs.collectAsState()
    val lastCommandSent by viewModel.lastCommandSent.collectAsState()
    val isTimeSynced by viewModel.isBleTimeSynced.collectAsState()

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
                    text = "ESP32-S3 BLE Hardware Integration",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Service UUID: ${BleManager.SERVICE_UUID}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Data Notify UUID: ${BleManager.CHARACTERISTIC_UUID}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Command UUID: ${BleManager.COMMAND_CHARACTERISTIC_UUID}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Status Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
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
                            val dotColor = when (connectionState) {
                                BleConnectionState.NOTIFICATION_READY, BleConnectionState.CONNECTED -> StatusNormal
                                BleConnectionState.SCANNING, BleConnectionState.CONNECTING -> StatusWarning
                                else -> Color(0xFF747D8C)
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "State: ${connectionState.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (connectionState == BleConnectionState.SCANNING) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (connectionState == BleConnectionState.SCANNING) {
                                    viewModel.stopBleScan()
                                } else {
                                    viewModel.startBleScan()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Icon(
                                imageVector = if (connectionState == BleConnectionState.SCANNING) Icons.Default.Refresh else Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (connectionState == BleConnectionState.SCANNING) "Stop Scan" else "Scan Devices",
                                fontSize = 12.sp
                            )
                        }

                        if (connectionState == BleConnectionState.CONNECTED || connectionState == BleConnectionState.NOTIFICATION_READY) {
                            Button(
                                onClick = { viewModel.disconnectBle() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusAlert)
                            ) {
                                Text(text = "Disconnect", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // ESP32-S3 BLE Remote Command Center
        item {
            BleCommandControlCard(
                connectionState = connectionState,
                isTimeSynced = isTimeSynced,
                lastCommandSent = lastCommandSent,
                commandLogs = commandLogs,
                onSendCommand = { viewModel.sendBleCommand(it) },
                onSendStart = { viewModel.sendBleStart() },
                onSendStop = { viewModel.sendBleStop() },
                onSendAutoOn = { viewModel.sendBleAutoOn() },
                onSendAutoOff = { viewModel.sendBleAutoOff() },
                onSendRead = { viewModel.sendBleRead() },
                onSyncTime = { viewModel.sendBleSyncTime() }
            )
        }

        // Local Telemetry Storage Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Telemetry Data Storage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All telemetry displayed in the application is strictly ingested from your connected ESP32-S3 wearable monitor. When disconnected, sensors remain in clean standby.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { viewModel.clearAllData() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Clear Local Telemetry History", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.triggerSimulatedAbnormalAlert() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Test Abnormal Telemetry & Audio Alert", fontSize = 12.sp, color = Color(0xFFFF5252))
                    }
                    if (exportMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = exportMessage!!,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimary
                        )
                    }
                }
            }
        }

        // Raw Incoming BLE JSON Viewer
        item {
            Text(
                text = "Live Ingested Telemetry Payload",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UART GATT RX BUFFER",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (rawPayload.isNotEmpty()) rawPayload else "// Waiting for incoming BLE notifications...",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        // Nearby Scanned BLE Devices List
        item {
            Text(
                text = "Discovered Devices (${scannedDevices.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (scannedDevices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No devices detected. Tap 'Scan Devices' to discover nearby ESP32-S3 monitors.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(scannedDevices) { device ->
                DeviceListItem(
                    device = device,
                    onConnect = { viewModel.connectBle(device.address) }
                )
            }
        }
    }
}

@Composable
fun BleCommandControlCard(
    connectionState: BleConnectionState,
    isTimeSynced: Boolean,
    lastCommandSent: String?,
    commandLogs: List<BleCommandLogEntry>,
    onSendCommand: (String) -> Unit,
    onSendStart: () -> Unit,
    onSendStop: () -> Unit,
    onSendAutoOn: () -> Unit,
    onSendAutoOff: () -> Unit,
    onSendRead: () -> Unit,
    onSyncTime: () -> Unit
) {
    val isConnected = connectionState == BleConnectionState.CONNECTED || connectionState == BleConnectionState.NOTIFICATION_READY
    var customCommandText by remember { mutableStateOf("") }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isConnected) TealPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TealPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Wearable Remote Commands",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Characteristic: 6e400004-b5a3-...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isConnected) StatusNormal.copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isConnected) "GATT READY" else "OFFLINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) StatusNormal else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clock Synchronization Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isTimeSynced) StatusNormal.copy(alpha = 0.1f)
                        else if (isConnected) StatusWarning.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isTimeSynced) StatusNormal.copy(alpha = 0.3f)
                        else if (isConnected) StatusWarning.copy(alpha = 0.3f)
                        else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTimeSynced) Icons.Default.CheckCircle else Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (isTimeSynced) StatusNormal else if (isConnected) StatusWarning else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isTimeSynced) "Clock Synchronized via BLE" else "Wearable Clock Sync",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isTimeSynced) "Phone Unix epoch sent automatically on connect" else "Auto-synchronizes phone timestamp on connection",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSyncTime,
                        enabled = isConnected,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text = "Sync Now", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6 Interactive Command Buttons
            Text(
                text = "DIRECT BLE COMMAND ACTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: START and STOP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CommandActionButton(
                    title = "START",
                    description = "Turn on monitoring",
                    icon = Icons.Default.PlayArrow,
                    containerColor = TealPrimary,
                    enabled = isConnected,
                    onClick = onSendStart,
                    modifier = Modifier.weight(1f)
                )

                CommandActionButton(
                    title = "STOP",
                    description = "Stop current session",
                    icon = Icons.Default.Stop,
                    containerColor = StatusAlert,
                    enabled = isConnected,
                    onClick = onSendStop,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: READ and TIME
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CommandActionButton(
                    title = "READ",
                    description = "Fetch latest readings",
                    icon = Icons.Default.Refresh,
                    containerColor = Color(0xFF0284C7),
                    enabled = isConnected,
                    onClick = onSendRead,
                    modifier = Modifier.weight(1f)
                )

                CommandActionButton(
                    title = "TIME:UNIX",
                    description = "Sync clock with phone",
                    icon = Icons.Default.AccessTime,
                    containerColor = Color(0xFF6366F1),
                    enabled = isConnected,
                    onClick = onSyncTime,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: AUTO_ON and AUTO_OFF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CommandActionButton(
                    title = "AUTO_ON",
                    description = "Enable auto schedule",
                    icon = Icons.Default.Schedule,
                    containerColor = Color(0xFF0D9488),
                    enabled = isConnected,
                    onClick = onSendAutoOn,
                    modifier = Modifier.weight(1f)
                )

                CommandActionButton(
                    title = "AUTO_OFF",
                    description = "Disable auto schedule",
                    icon = Icons.Default.TimerOff,
                    containerColor = Color(0xFF475569),
                    enabled = isConnected,
                    onClick = onSendAutoOff,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom BLE Command Sender
            Text(
                text = "CUSTOM COMMAND CONSOLE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customCommandText,
                    onValueChange = { customCommandText = it },
                    placeholder = { Text("e.g. START, READ, TIME:1729800000", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Button(
                    onClick = {
                        val text = customCommandText.trim()
                        if (text.isNotEmpty()) {
                            val cmd = if (text.equals("TIME:NOW", ignoreCase = true)) {
                                "TIME:${System.currentTimeMillis() / 1000L}"
                            } else text
                            onSendCommand(cmd)
                            customCommandText = ""
                        }
                    },
                    enabled = isConnected && customCommandText.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Send", fontSize = 12.sp)
                }
            }

            // Quick suggestion chips
            Spacer(modifier = Modifier.height(8.dp))
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val chips = listOf("START", "STOP", "READ", "AUTO_ON", "AUTO_OFF", "TIME:NOW")
                for (chip in chips) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { customCommandText = chip }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = chip,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Command Transmission Audit Log
            if (commandLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COMMAND AUDIT LOG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${commandLogs.size} recorded",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (log in commandLogs.take(5)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (log.success) StatusNormal else StatusAlert)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.command,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.success) Color(0xFF38BDF8) else Color(0xFFF87171)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${log.details}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = timeFormat.format(Date(log.timestampMs)),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommandActionButton(
    title: String,
    description: String,
    icon: ImageVector,
    containerColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) containerColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (enabled) containerColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (enabled) containerColor else Color(0xFF64748B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DeviceListItem(
    device: BleDeviceItem,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (device.isS3Wearable) 1.5.dp else 1.dp,
                color = if (device.isS3Wearable) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (device.isS3Wearable) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TealPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "Target Wearable", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${device.address} • RSSI: ${device.rssi} dBm",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onConnect,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(text = "Connect", fontSize = 11.sp)
            }
        }
    }
}
