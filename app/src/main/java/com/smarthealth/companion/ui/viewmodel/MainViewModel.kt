package com.smarthealth.companion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthealth.companion.export.ShareHelper
import com.smarthealth.companion.model.AlertSeverity
import com.smarthealth.companion.model.AnalyticsSummary
import com.smarthealth.companion.model.BleCommandLogEntry
import com.smarthealth.companion.model.BleConnectionState
import com.smarthealth.companion.model.BleDeviceItem
import com.smarthealth.companion.model.ForecastResult
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.model.RealtimeAlertItem
import com.smarthealth.companion.model.RespiratorySymptomRecord
import com.smarthealth.companion.model.TriMetricEnvironmentAnalysis
import com.smarthealth.companion.model.UserProfile
import com.smarthealth.companion.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(private val repository: HealthRepository) : ViewModel() {

    val bleConnectionState: StateFlow<BleConnectionState> = repository.bleConnectionState
    val scannedDevices: StateFlow<List<BleDeviceItem>> = repository.scannedDevices
    val lastSyncTimestampMs: StateFlow<Long?> = repository.lastSyncTimestampMs
    val rawBlePayload: StateFlow<String> = repository.rawBlePayload
    val bleCommandLogs: StateFlow<List<BleCommandLogEntry>> = repository.bleCommandLogs
    val lastCommandSent: StateFlow<String?> = repository.lastCommandSent
    val isBleTimeSynced: StateFlow<Boolean> = repository.isBleTimeSynced
    val latestMeasurements: StateFlow<Map<MeasurementType, Measurement>> = repository.latestMeasurements
    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val lastKnownLocation = repository.lastKnownLocation

    // Analysis Duration: 1, 3, 7, 14, 21, 28, 30 days
    private val _selectedDurationDays = MutableStateFlow(7)
    val selectedDurationDays: StateFlow<Int> = _selectedDurationDays.asStateFlow()

    // Active Metric for detailed charts and analysis
    private val _selectedMetricType = MutableStateFlow(MeasurementType.HEART_RATE)
    val selectedMetricType: StateFlow<MeasurementType> = _selectedMetricType.asStateFlow()

    // Forecast Horizon: 6, 24, 72 (3 days), 168 (7 days)
    private val _selectedForecastHorizonHours = MutableStateFlow(24)
    val selectedForecastHorizonHours: StateFlow<Int> = _selectedForecastHorizonHours.asStateFlow()

    private val _currentAnalyticsSummary = MutableStateFlow<AnalyticsSummary?>(null)
    val currentAnalyticsSummary: StateFlow<AnalyticsSummary?> = _currentAnalyticsSummary.asStateFlow()

    private val _triMetricAnalysis = MutableStateFlow<TriMetricEnvironmentAnalysis?>(null)
    val triMetricAnalysis: StateFlow<TriMetricEnvironmentAnalysis?> = _triMetricAnalysis.asStateFlow()

    val activeAlert: StateFlow<RealtimeAlertItem?> = repository.alertManager.activeAlert

    private val _currentForecastResult = MutableStateFlow<ForecastResult?>(null)
    val currentForecastResult: StateFlow<ForecastResult?> = _currentForecastResult.asStateFlow()

    private val _aiSummaryText = MutableStateFlow<String>("")
    val aiSummaryText: StateFlow<String> = _aiSummaryText.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage: StateFlow<String?> = _exportStatusMessage.asStateFlow()

    private val _chartMeasurements = MutableStateFlow<List<Measurement>>(emptyList())
    val chartMeasurements: StateFlow<List<Measurement>> = _chartMeasurements.asStateFlow()

    private val _triMetricComparisonMeasurements = MutableStateFlow<Map<MeasurementType, List<Measurement>>>(emptyMap())
    val triMetricComparisonMeasurements: StateFlow<Map<MeasurementType, List<Measurement>>> = _triMetricComparisonMeasurements.asStateFlow()

    private var chartJob: kotlinx.coroutines.Job? = null
    private var comparisonJob: kotlinx.coroutines.Job? = null

    val symptomRecords: StateFlow<List<RespiratorySymptomRecord>> = repository.getAllSymptomsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExplorerMeasurements: StateFlow<List<Measurement>> = repository.getAllMeasurementsFlow(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshAnalytics()
        refreshChartData()

        viewModelScope.launch {
            userProfile.collect {
                refreshAnalytics()
            }
        }
    }

    fun selectDuration(days: Int) {
        _selectedDurationDays.value = days
        refreshAnalytics()
        refreshChartData()
    }

    fun selectMetric(type: MeasurementType) {
        _selectedMetricType.value = type
        refreshAnalytics()
        refreshChartData()
    }

    fun selectForecastHorizon(hours: Int) {
        _selectedForecastHorizonHours.value = hours
        refreshAnalytics()
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            val duration = _selectedDurationDays.value
            val metric = _selectedMetricType.value
            val horizon = _selectedForecastHorizonHours.value

            val summary = repository.computeAnalyticsSummary(metric, duration)
            _currentAnalyticsSummary.value = summary

            val triMetric = repository.computeTriMetricAnalysis(duration)
            _triMetricAnalysis.value = triMetric

            val forecast = repository.computeForecast(metric, horizon, duration)
            _currentForecastResult.value = forecast

            val aiText = repository.generateAiSummary(duration)
            _aiSummaryText.value = aiText
        }
    }

    fun refreshChartData() {
        chartJob?.cancel()
        chartJob = viewModelScope.launch {
            repository.getMeasurementsFlow(_selectedMetricType.value, _selectedDurationDays.value)
                .collect { list ->
                    _chartMeasurements.value = list
                }
        }

        comparisonJob?.cancel()
        comparisonJob = viewModelScope.launch {
            repository.getTriMetricMeasurementsFlow(_selectedDurationDays.value)
                .collect { map ->
                    _triMetricComparisonMeasurements.value = map
                }
        }
    }

    fun dismissAlert() {
        repository.alertManager.dismissActiveAlert()
    }

    fun triggerSimulatedAbnormalAlert() {
        repository.alertManager.triggerAlert(
            RealtimeAlertItem(
                title = "⚠️ Abnormal Space Pressure & Vitals: Relocate",
                message = "Abnormal atmosphere pressure (962 hPa), heart rate (114 BPM), and temp (38.1 °C).",
                guidance = "Move somewhere else, the space has abnormal pressure, your oxygen level is reduced. Please move somewhere else from this. The pressure is more higher and lower. Your oxygen level, heart rate level has abnormal, and temperature goes abnormal. Please move somewhere else.",
                primaryMetric = MeasurementType.ATMOSPHERIC_PRESSURE,
                severity = AlertSeverity.URGENT,
                pressureValue = 962.0,
                heartRateValue = 114.0,
                temperatureValue = 38.1
            )
        )
    }

    // BLE Actions
    fun startBleScan() = repository.bleManager.startScan()
    fun stopBleScan() = repository.bleManager.stopScan()
    fun connectBle(address: String) = repository.bleManager.connect(address)
    fun disconnectBle() = repository.bleManager.disconnect()
    fun sendBleCommand(command: String) = repository.sendBleCommand(command)
    fun sendBleStart() = repository.sendBleStart()
    fun sendBleStop() = repository.sendBleStop()
    fun sendBleAutoOn() = repository.sendBleAutoOn()
    fun sendBleAutoOff() = repository.sendBleAutoOff()
    fun sendBleRead() = repository.sendBleRead()
    fun sendBleSyncTime() = repository.sendBleSyncTime()

    fun toggleGpsLogging(enabled: Boolean) {
        repository.setGpsLogging(enabled)
    }

    fun saveProfile(profile: UserProfile) {
        repository.saveUserProfile(profile)
    }

    fun logSymptomRecord(record: RespiratorySymptomRecord) {
        viewModelScope.launch {
            repository.recordSymptom(record)
            refreshAnalytics()
        }
    }

    fun exportAndSharePdf(context: Context) {
        viewModelScope.launch {
            _exportStatusMessage.value = "Generating clinical PDF report..."
            try {
                val pdfFile = repository.generatePdfReport(_selectedDurationDays.value)
                _exportStatusMessage.value = "Report generated successfully."
                ShareHelper.shareFile(context, pdfFile, "application/pdf", "Smart Health Companion Report")
            } catch (e: Exception) {
                _exportStatusMessage.value = "Failed to generate PDF: ${e.message}"
            }
        }
    }

    fun exportAndShareCsv(context: Context) {
        viewModelScope.launch {
            _exportStatusMessage.value = "Exporting raw telemetry CSV..."
            try {
                val csvFile = repository.exportCsv(_selectedDurationDays.value)
                _exportStatusMessage.value = "CSV exported successfully."
                ShareHelper.shareFile(context, csvFile, "text/csv", "Health Telemetry Data CSV")
            } catch (e: Exception) {
                _exportStatusMessage.value = "Failed to export CSV: ${e.message}"
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            refreshAnalytics()
            refreshChartData()
            _exportStatusMessage.value = "All stored telemetry and logs cleared."
        }
    }

    fun clearExportMessage() {
        _exportStatusMessage.value = null
    }
}
