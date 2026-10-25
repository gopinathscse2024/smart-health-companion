package com.smarthealth.companion.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.smarthealth.companion.model.BleCommandLogEntry
import com.smarthealth.companion.model.BleConnectionState
import com.smarthealth.companion.model.BleDeviceItem
import com.smarthealth.companion.model.Measurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Production-ready Bluetooth Low Energy Manager for the ESP32-S3 Wearable Healthcare Monitor.
 *
 * Target Service UUID: 6e400001-b5a3-f393-e0a9-e50e24dcca9e
 * Target Data Characteristic UUID: 6e400003-b5a3-f393-e0a9-e50e24dcca9e
 * Target Command Characteristic UUID: 6e400004-b5a3-f393-e0a9-e50e24dcca9e
 * Expected Device Name: "S3 Health Monitor"
 */
class BleManager(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val COMMAND_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400004-b5a3-f393-e0a9-e50e24dcca9e")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val TARGET_DEVICE_NAME = "S3 Health Monitor"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val parser = BleJsonParser()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDeviceItem>>(emptyList())
    val scannedDevices: StateFlow<List<BleDeviceItem>> = _scannedDevices.asStateFlow()

    private val _incomingMeasurements = MutableSharedFlow<List<Measurement>>(extraBufferCapacity = 64)
    val incomingMeasurements: SharedFlow<List<Measurement>> = _incomingMeasurements.asSharedFlow()

    private val _lastSyncTimestampMs = MutableStateFlow<Long?>(null)
    val lastSyncTimestampMs: StateFlow<Long?> = _lastSyncTimestampMs.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _rawIncomingPayload = MutableStateFlow<String>("")
    val rawIncomingPayload: StateFlow<String> = _rawIncomingPayload.asStateFlow()

    private val _commandLogs = MutableStateFlow<List<BleCommandLogEntry>>(emptyList())
    val commandLogs: StateFlow<List<BleCommandLogEntry>> = _commandLogs.asStateFlow()

    private val _lastCommandSent = MutableStateFlow<String?>(null)
    val lastCommandSent: StateFlow<String?> = _lastCommandSent.asStateFlow()

    private val _isTimeSynced = MutableStateFlow<Boolean>(false)
    val isTimeSynced: StateFlow<Boolean> = _isTimeSynced.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var lastTargetAddress: String? = null
    private var isScanning = false
    private var shouldAutoReconnect = true

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val name = device.name ?: result.scanRecord?.deviceName ?: "Unknown BLE Device"
                val address = device.address
                val rssi = result.rssi
                val isS3 = name.contains("S3", ignoreCase = true) || name.contains("Health", ignoreCase = true)
                android.util.Log.d("BleManager", "Found device: $name ($address), RSSI=$rssi, isS3=$isS3")

                val current = _scannedDevices.value.toMutableList()
                val existingIndex = current.indexOfFirst { it.address == address }
                val item = BleDeviceItem(name = name, address = address, rssi = rssi, isS3Wearable = isS3)

                if (existingIndex >= 0) {
                    current[existingIndex] = item
                } else {
                    if (isS3) current.add(0, item) else current.add(item)
                }
                _scannedDevices.value = current
            }
        }

        override fun onScanFailed(errorCode: Int) {
            android.util.Log.e("BleManager", "Scan failed with error code: $errorCode")
            _connectionState.value = BleConnectionState.ERROR
            isScanning = false
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _connectionState.value = BleConnectionState.CONNECTED
                        _connectedDeviceName.value = gatt?.device?.name ?: TARGET_DEVICE_NAME
                        android.util.Log.d("BleManager", "Connected to GATT. Requesting MTU 512...")
                        val mtuRequested = gatt?.requestMtu(512) ?: false
                        android.util.Log.d("BleManager", "requestMtu(512) initiated: $mtuRequested")
                        if (!mtuRequested) {
                            _connectionState.value = BleConnectionState.DISCOVERING_SERVICES
                            gatt?.discoverServices()
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _connectionState.value = BleConnectionState.DISCONNECTED
                        _connectedDeviceName.value = null
                        commandCharacteristic = null
                        dataCharacteristic = null
                        _isTimeSynced.value = false
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                        triggerAutoReconnect()
                    }
                }
            } else {
                _connectionState.value = BleConnectionState.DISCONNECTED
                _connectedDeviceName.value = null
                commandCharacteristic = null
                dataCharacteristic = null
                _isTimeSynced.value = false
                bluetoothGatt?.close()
                bluetoothGatt = null
                triggerAutoReconnect()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            android.util.Log.d("BleManager", "onMtuChanged: mtu=$mtu, status=$status")
            _connectionState.value = BleConnectionState.DISCOVERING_SERVICES
            gatt?.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            android.util.Log.d("BleManager", "onServicesDiscovered status=$status, gatt=$gatt")
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                // Find service matching target UUID (case-insensitive fallback)
                val service = gatt.getService(SERVICE_UUID)
                    ?: gatt.services.firstOrNull { it.uuid.toString().equals(SERVICE_UUID.toString(), ignoreCase = true) }
                    ?: gatt.services.firstOrNull { it.getCharacteristic(CHARACTERISTIC_UUID) != null }

                if (service != null) {
                    val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                        ?: service.characteristics.firstOrNull { it.uuid.toString().equals(CHARACTERISTIC_UUID.toString(), ignoreCase = true) }

                    var cmdChar = service.getCharacteristic(COMMAND_CHARACTERISTIC_UUID)
                        ?: service.characteristics.firstOrNull { it.uuid.toString().equals(COMMAND_CHARACTERISTIC_UUID.toString(), ignoreCase = true) }

                    if (cmdChar == null) {
                        for (s in gatt.services) {
                            cmdChar = s.getCharacteristic(COMMAND_CHARACTERISTIC_UUID)
                                ?: s.characteristics.firstOrNull { it.uuid.toString().equals(COMMAND_CHARACTERISTIC_UUID.toString(), ignoreCase = true) }
                            if (cmdChar != null) break
                        }
                    }

                    dataCharacteristic = characteristic
                    commandCharacteristic = cmdChar
                    android.util.Log.d("BleManager", "Services discovered. DataChar=$characteristic, CmdChar=$cmdChar")

                    if (characteristic != null) {
                        val notifySet = gatt.setCharacteristicNotification(characteristic, true)
                        android.util.Log.d("BleManager", "setCharacteristicNotification success=$notifySet")

                        val descriptor = characteristic.getDescriptor(CCCD_UUID)
                            ?: characteristic.descriptors.firstOrNull()
                        if (descriptor != null) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            } else {
                                @Suppress("DEPRECATION")
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                @Suppress("DEPRECATION")
                                gatt.writeDescriptor(descriptor)
                            }
                            android.util.Log.d("BleManager", "CCCD descriptor write initiated")
                        } else {
                            _connectionState.value = BleConnectionState.NOTIFICATION_READY
                            // No CCCD required - trigger automatic time sync immediately
                            scope.launch {
                                delay(250L)
                                syncPhoneTime()
                            }
                        }
                    } else {
                        android.util.Log.e("BleManager", "Characteristic $CHARACTERISTIC_UUID not found")
                        _connectionState.value = BleConnectionState.ERROR
                    }
                } else {
                    android.util.Log.e("BleManager", "Service $SERVICE_UUID not found in ${gatt.services.map { it.uuid }}")
                    _connectionState.value = BleConnectionState.ERROR
                }
            } else {
                android.util.Log.e("BleManager", "Service discovery failed with status $status")
                _connectionState.value = BleConnectionState.ERROR
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            android.util.Log.d("BleManager", "onDescriptorWrite: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.NOTIFICATION_READY
                // Important: The Android app must send TIME: with current Unix timestamp when Bluetooth connects.
                scope.launch {
                    delay(200L)
                    syncPhoneTime()
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val success = status == BluetoothGatt.GATT_SUCCESS
            android.util.Log.d("BleManager", "onCharacteristicWrite: uuid=${characteristic?.uuid}, status=$status, success=$success")
        }

        @Deprecated("Deprecated for API 33+")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val data = characteristic?.value ?: return
            handleIncomingBytes(data, gatt?.device?.address)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBytes(value, gatt.device.address)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleIncomingBytes(value, gatt.device.address)
            }
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray, deviceAddress: String?) {
        val text = String(bytes, Charsets.UTF_8)
        android.util.Log.d("BleManager", "BLE RX: $text")
        _rawIncomingPayload.value = text
        _lastSyncTimestampMs.value = System.currentTimeMillis()

        val parsed = parser.ingestBytes(bytes, deviceAddress)
        if (parsed.isNotEmpty()) {
            android.util.Log.d("BleManager", "Parsed ${parsed.size} measurements: ${parsed.map { "${it.type}=${it.numericValue}${it.unit}" }}")
            scope.launch {
                _incomingMeasurements.emit(parsed)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            return
        }
        if (!isScanning) {
            _scannedDevices.value = emptyList()
            _connectionState.value = BleConnectionState.SCANNING
            isScanning = true
            val settings = android.bluetooth.le.ScanSettings.Builder()
                .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()
            bluetoothAdapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)

            // Auto stop scan after 15 seconds to save battery
            scope.launch {
                delay(15000L)
                if (isScanning && _connectionState.value == BleConnectionState.SCANNING) {
                    stopScan()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isScanning) {
            isScanning = false
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            if (_connectionState.value == BleConnectionState.SCANNING) {
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        stopScan()
        lastTargetAddress = address
        shouldAutoReconnect = true
        val device: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(address) ?: return

        _connectionState.value = BleConnectionState.CONNECTING
        bluetoothGatt?.close()
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        shouldAutoReconnect = false
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        commandCharacteristic = null
        dataCharacteristic = null
        _isTimeSynced.value = false
        _connectionState.value = BleConnectionState.DISCONNECTED
        _connectedDeviceName.value = null
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String): Boolean {
        val gatt = bluetoothGatt
        val cmdChar = commandCharacteristic
        if (gatt == null || cmdChar == null) {
            val reason = if (gatt == null) "Wearable GATT not connected" else "Command characteristic (6e400004) not available"
            android.util.Log.e("BleManager", "Cannot send command '$command': $reason")
            addCommandLog(command, false, reason)
            return false
        }

        val bytes = command.toByteArray(Charsets.UTF_8)
        val success = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val writeType = if ((cmdChar.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                } else {
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }
                val statusCode = gatt.writeCharacteristic(cmdChar, bytes, writeType)
                statusCode == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                cmdChar.value = bytes
                if ((cmdChar.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                    cmdChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                } else {
                    cmdChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(cmdChar)
            }
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "Exception writing characteristic: ${e.message}", e)
            false
        }

        if (command.startsWith("TIME:") && success) {
            _isTimeSynced.value = true
        }
        _lastCommandSent.value = command
        addCommandLog(
            command = command,
            success = success,
            details = if (success) "Transmitted to ESP32-S3 (Characteristic $COMMAND_CHARACTERISTIC_UUID)" else "GATT write returned failure"
        )
        android.util.Log.d("BleManager", "sendCommand '$command' initiated: $success")
        return success
    }

    fun sendStart(): Boolean = sendCommand("START")
    fun sendStop(): Boolean = sendCommand("STOP")
    fun sendAutoOn(): Boolean = sendCommand("AUTO_ON")
    fun sendAutoOff(): Boolean = sendCommand("AUTO_OFF")
    fun sendRead(): Boolean = sendCommand("READ")

    fun syncPhoneTime(unixSeconds: Long = System.currentTimeMillis() / 1000L): Boolean {
        val cmd = "TIME:$unixSeconds"
        android.util.Log.d("BleManager", "Sending clock synchronization: $cmd")
        return sendCommand(cmd)
    }

    private fun addCommandLog(command: String, success: Boolean, details: String) {
        val entry = BleCommandLogEntry(command = command, success = success, details = details)
        _commandLogs.value = (listOf(entry) + _commandLogs.value).take(40)
    }

    private fun triggerAutoReconnect() {
        if (shouldAutoReconnect && lastTargetAddress != null) {
            scope.launch {
                _connectionState.value = BleConnectionState.RECONNECTING
                delay(3000L)
                lastTargetAddress?.let { connect(it) }
            }
        }
    }
}
