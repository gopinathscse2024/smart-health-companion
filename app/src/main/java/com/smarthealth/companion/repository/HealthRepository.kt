package com.smarthealth.companion.repository

import com.smarthealth.companion.alert.RealtimeAlertManager
import com.smarthealth.companion.analytics.AiSummaryGenerator
import com.smarthealth.companion.analytics.BaselineAnalyzer
import com.smarthealth.companion.analytics.FrequentDataAnomalyEngine
import com.smarthealth.companion.analytics.PredictiveEngine
import com.smarthealth.companion.analytics.StatisticalEngine
import com.smarthealth.companion.analytics.TriMetricAnalysisEngine
import com.smarthealth.companion.data.ble.BleManager
import com.smarthealth.companion.data.ble.SampleDataGenerator
import com.smarthealth.companion.data.local.MeasurementDao
import com.smarthealth.companion.data.local.MeasurementEntity
import com.smarthealth.companion.data.local.PreferenceManager
import com.smarthealth.companion.data.local.SymptomRecordDao
import com.smarthealth.companion.data.local.SymptomRecordEntity
import com.smarthealth.companion.data.location.GpsLocationData
import com.smarthealth.companion.data.location.LocationProvider
import com.smarthealth.companion.export.CsvExporter
import com.smarthealth.companion.export.PdfReportGenerator
import com.smarthealth.companion.model.AlertSeverity
import com.smarthealth.companion.model.AnalyticsSummary
import com.smarthealth.companion.model.AnomalyLevel
import com.smarthealth.companion.model.BleCommandLogEntry
import com.smarthealth.companion.model.BleConnectionState
import com.smarthealth.companion.model.BleDeviceItem
import com.smarthealth.companion.model.DataSource
import com.smarthealth.companion.model.ForecastResult
import com.smarthealth.companion.model.Measurement
import com.smarthealth.companion.model.MeasurementType
import com.smarthealth.companion.model.RealtimeAlertItem
import com.smarthealth.companion.model.RespiratorySymptomRecord
import com.smarthealth.companion.model.TrendDirection
import com.smarthealth.companion.model.TriMetricEnvironmentAnalysis
import com.smarthealth.companion.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class HealthRepository(
    private val measurementDao: MeasurementDao,
    private val symptomDao: SymptomRecordDao,
    val bleManager: BleManager,
    private val preferenceManager: PreferenceManager,
    private val locationProvider: LocationProvider,
    private val pdfReportGenerator: PdfReportGenerator,
    private val csvExporter: CsvExporter,
    val alertManager: RealtimeAlertManager
) {
    private val sampleGenerator = SampleDataGenerator()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val anomalyEngine = FrequentDataAnomalyEngine()

    val userProfile: StateFlow<UserProfile> = preferenceManager.userProfileFlow
    val bleConnectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val scannedDevices: StateFlow<List<BleDeviceItem>> = bleManager.scannedDevices
    val lastSyncTimestampMs: StateFlow<Long?> = bleManager.lastSyncTimestampMs
    val rawBlePayload: StateFlow<String> = bleManager.rawIncomingPayload
    val bleCommandLogs: StateFlow<List<BleCommandLogEntry>> = bleManager.commandLogs
    val lastCommandSent: StateFlow<String?> = bleManager.lastCommandSent
    val isBleTimeSynced: StateFlow<Boolean> = bleManager.isTimeSynced
    val lastKnownLocation: StateFlow<GpsLocationData?> = locationProvider.lastKnownLocation

    fun sendBleCommand(command: String): Boolean = bleManager.sendCommand(command)
    fun sendBleStart(): Boolean = bleManager.sendStart()
    fun sendBleStop(): Boolean = bleManager.sendStop()
    fun sendBleAutoOn(): Boolean = bleManager.sendAutoOn()
    fun sendBleAutoOff(): Boolean = bleManager.sendAutoOff()
    fun sendBleRead(): Boolean = bleManager.sendRead()
    fun sendBleSyncTime(): Boolean = bleManager.syncPhoneTime()

    // Real-time map of latest measurement for each measurement type
    private val _latestMeasurements = MutableStateFlow<Map<MeasurementType, Measurement>>(emptyMap())
    val latestMeasurements: StateFlow<Map<MeasurementType, Measurement>> = _latestMeasurements.asStateFlow()

    init {
        // Ensure simulation mode is permanently disabled and purge any lingering simulated records
        preferenceManager.setSimulationMode(false)
        scope.launch {
            measurementDao.deleteBySource(DataSource.SIMULATED.name)
        }

        // Collect incoming real BLE measurements and persist to Room with baseline evaluation
        scope.launch {
            bleManager.incomingMeasurements.collect { measurements ->
                processAndSaveMeasurements(measurements)
            }
        }
    }

    private suspend fun processAndSaveMeasurements(measurements: List<Measurement>) {
        val profile = userProfile.value
        val gps = if (profile.enableGpsLogging) {
            locationProvider.getCurrentLocation(true)
        } else null

        val entitiesToSave = mutableListOf<MeasurementEntity>()
        val latestMap = _latestMeasurements.value.toMutableMap()

        for (m in measurements) {
            // Feed rolling telemetry engine to analyze frequent data stream and detect sudden rises/plunges
            anomalyEngine.ingestSample(m.type, m.numericValue)

            // Retrieve recent historical valid readings for baseline comparison
            val cutoff = System.currentTimeMillis() - (7 * 86400_000L)
            val history = measurementDao.getListByTypeAndRange(m.type.name, cutoff, System.currentTimeMillis())
            val historyValues = history.map { it.numericValue }

            val evaluation = BaselineAnalyzer.evaluateMeasurement(m, historyValues, profile)

            val enriched = m.copy(
                latitude = gps?.latitude,
                longitude = gps?.longitude,
                locationAccuracy = gps?.accuracyMeters,
                locationTimestampMs = gps?.timestampMs,
                baselineComparison = evaluation.comparison
            )

            latestMap[enriched.type] = enriched
            entitiesToSave.add(MeasurementEntity.fromDomain(enriched))
        }

        measurementDao.insertAll(entitiesToSave)
        _latestMeasurements.value = latestMap

        // Real-time evaluation of continuous frequent telemetry:
        // - Small changes and mild drifts ("normal-abnormal, it's okay") remain SILENT.
        // - ONLY sudden rises or extreme breaches ("over-abnormal") activate notifications,
        //   gentle sound touch, and alertment system.
        // - Auto-dismisses alert banner when readings recover from over-abnormal.
        val anomalyResult = anomalyEngine.evaluateFrequentData()
        if (anomalyResult.level == AnomalyLevel.OVER_ABNORMAL && anomalyResult.triggerAlert != null) {
            alertManager.triggerAlert(anomalyResult.triggerAlert)
        } else if (anomalyResult.shouldDismissActiveAlert) {
            alertManager.dismissActiveAlert()
        }
    }

    fun setSimulationMode(enabled: Boolean) {
        preferenceManager.setSimulationMode(false)
    }

    fun setGpsLogging(enabled: Boolean) {
        preferenceManager.setGpsLogging(enabled)
    }

    fun saveUserProfile(profile: UserProfile) {
        preferenceManager.saveProfile(profile)
    }

    suspend fun seedDemonstrationHistory() {
        val sampleHistory = sampleGenerator.generate30DayHistoricalData()
        val entities = sampleHistory.map { MeasurementEntity.fromDomain(it) }
        measurementDao.insertAll(entities)

        val sampleSymptoms = sampleGenerator.generateSampleRespiratoryLogs()
        for (s in sampleSymptoms) {
            symptomDao.insert(SymptomRecordEntity.fromDomain(s))
        }
    }

    fun getMeasurementsFlow(type: MeasurementType, durationDays: Int): Flow<List<Measurement>> {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - (durationDays * 86400_000L)
        return measurementDao.getMeasurementsByTypeAndRange(type.name, startMs, endMs)
            .map { list -> list.map { it.toDomain() } }
    }

    fun getAllMeasurementsFlow(durationDays: Int): Flow<List<Measurement>> {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - (durationDays * 86400_000L)
        return measurementDao.getAllMeasurementsByRange(startMs, endMs)
            .map { list -> list.map { it.toDomain() } }
    }

    fun getTriMetricMeasurementsFlow(durationDays: Int): Flow<Map<MeasurementType, List<Measurement>>> {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - (durationDays * 86400_000L)
        return combine(
            measurementDao.getMeasurementsByTypeAndRange(MeasurementType.HEART_RATE.name, startMs, endMs),
            measurementDao.getMeasurementsByTypeAndRange(MeasurementType.ATMOSPHERIC_PRESSURE.name, startMs, endMs),
            measurementDao.getMeasurementsByTypeAndRange(MeasurementType.BODY_TEMPERATURE.name, startMs, endMs)
        ) { hr, pressure, temp ->
            mapOf(
                MeasurementType.HEART_RATE to hr.map { it.toDomain() },
                MeasurementType.ATMOSPHERIC_PRESSURE to pressure.map { it.toDomain() },
                MeasurementType.BODY_TEMPERATURE to temp.map { it.toDomain() }
            )
        }
    }

    fun getAllSymptomsFlow(): Flow<List<RespiratorySymptomRecord>> {
        return symptomDao.getAllSymptoms().map { list -> list.map { it.toDomain() } }
    }

    suspend fun recordSymptom(record: RespiratorySymptomRecord) {
        symptomDao.insert(SymptomRecordEntity.fromDomain(record))
    }

    suspend fun computeAnalyticsSummary(type: MeasurementType, durationDays: Int): AnalyticsSummary {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - (durationDays * 86400_000L)
        val entities = measurementDao.getListByTypeAndRange(type.name, startMs, endMs)
        val measurements = entities.map { it.toDomain() }

        val valid = measurements.filter { it.numericValue > 0 }
        val values = valid.map { it.numericValue }

        val avg = StatisticalEngine.calculateMean(values)
        val med = StatisticalEngine.calculateMedian(values)
        val min = if (values.isNotEmpty()) values.minOrNull() ?: 0.0 else 0.0
        val max = if (values.isNotEmpty()) values.maxOrNull() ?: 0.0 else 0.0
        val stdDev = StatisticalEngine.calculateStandardDeviation(values)
        val p25 = StatisticalEngine.calculatePercentile(values, 25.0)
        val p75 = StatisticalEngine.calculatePercentile(values, 75.0)
        val iqr = p75 - p25

        // Trend calculation
        val timestamps = valid.map { it.phoneReceiptTimestampMs }
        val (slopePerHour, _) = if (timestamps.isNotEmpty()) {
            val first = timestamps.first()
            val timeHours = timestamps.map { (it - first).toDouble() / 3600_000.0 }
            StatisticalEngine.calculateLinearRegression(timeHours, values)
        } else Pair(0.0, 0.0)

        val trendDirection = when {
            stdDev > (avg * 0.25).coerceAtLeast(4.0) -> TrendDirection.FLUCTUATING
            slopePerHour > 0.05 -> TrendDirection.RISING
            slopePerHour < -0.05 -> TrendDirection.FALLING
            else -> TrendDirection.STABLE
        }

        val anomalies = valid.count {
            it.baselineComparison == com.smarthealth.companion.model.BaselineComparison.UNUSUAL_CHANGE ||
                    it.baselineComparison == com.smarthealth.companion.model.BaselineComparison.URGENT_CONCERN
        }

        val expectedSamples = (durationDays * 24).coerceAtLeast(1)
        val completeness = ((valid.size.toDouble() / expectedSamples) * 100.0).coerceIn(0.0, 100.0)

        val durationLabel = when (durationDays) {
            1 -> "Last 24 Hours"
            3 -> "Last 3 Days"
            7 -> "Last 7 Days"
            14 -> "Last 14 Days"
            21 -> "Last 21 Days"
            28 -> "Last 28 Days"
            30 -> "Last 30 Days"
            else -> "Last $durationDays Days"
        }

        return AnalyticsSummary(
            measurementType = type,
            startDateMs = startMs,
            endDateMs = endMs,
            durationLabel = durationLabel,
            totalRecordedSamples = measurements.size,
            validSamples = valid.size,
            missingOrMalformedSamples = measurements.size - valid.size,
            average = avg,
            median = med,
            min = min,
            max = max,
            standardDeviation = stdDev,
            percentile25 = p25,
            percentile75 = p75,
            iqr = iqr,
            baselineMedian = med,
            baselineShiftPercent = if (med > 0) ((avg - med) / med) * 100.0 else 0.0,
            trendDirection = trendDirection,
            rateOfChangePerHour = slopePerHour,
            unusualMeasurementCount = anomalies,
            unusualDurationMinutes = (anomalies * 5).toLong(),
            dataCompletenessPercent = completeness,
            confidenceLevel = if (valid.size >= 30) "High Confidence" else "Moderate Confidence"
        )
    }

    suspend fun computeForecast(type: MeasurementType, horizonHours: Int, durationDays: Int = 14): ForecastResult {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - (durationDays * 86400_000L)
        val entities = measurementDao.getListByTypeAndRange(type.name, startMs, endMs)
        val measurements = entities.map { it.toDomain() }
        return PredictiveEngine.generateForecast(measurements, type, horizonHours)
    }

    suspend fun generateAiSummary(durationDays: Int): String {
        val hr = computeAnalyticsSummary(MeasurementType.HEART_RATE, durationDays)
        val temp = computeAnalyticsSummary(MeasurementType.BODY_TEMPERATURE, durationDays)
        val pressure = computeAnalyticsSummary(MeasurementType.ATMOSPHERIC_PRESSURE, durationDays)

        val endMs = System.currentTimeMillis()
        val startMs = endMs - (durationDays * 86400_000L)
        val symptoms = symptomDao.getListByRange(startMs, endMs).map { it.toDomain() }

        val label = when (durationDays) {
            1 -> "1-Day"
            3 -> "3-Day"
            7 -> "7-Day"
            14 -> "14-Day"
            21 -> "21-Day"
            28 -> "28-Day"
            30 -> "30-Day"
            else -> "$durationDays-Day"
        }

        return AiSummaryGenerator.generateSummary(hr, temp, pressure, symptoms, label)
    }

    suspend fun generatePdfReport(durationDays: Int): File {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - (durationDays * 86400_000L)
        val profile = userProfile.value

        val hrSummary = computeAnalyticsSummary(MeasurementType.HEART_RATE, durationDays)
        val tempSummary = computeAnalyticsSummary(MeasurementType.BODY_TEMPERATURE, durationDays)
        val pressureSummary = computeAnalyticsSummary(MeasurementType.ATMOSPHERIC_PRESSURE, durationDays)
        val altitudeSummary = computeAnalyticsSummary(MeasurementType.ESTIMATED_ALTITUDE, durationDays)

        val summaries = listOf(hrSummary, tempSummary, pressureSummary, altitudeSummary)
        val recentEntities = measurementDao.getListByRange(startMs, endMs)
        val recentMeasurements = recentEntities.map { it.toDomain() }
        val symptoms = symptomDao.getListByRange(startMs, endMs).map { it.toDomain() }

        val label = "$durationDays Days"
        return pdfReportGenerator.generateReport(
            profile = profile,
            durationLabel = label,
            startDateMs = startMs,
            endDateMs = endMs,
            summaries = summaries,
            recentMeasurements = recentMeasurements,
            symptomLogs = symptoms
        )
    }

    suspend fun exportCsv(durationDays: Int): File {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - (durationDays * 86400_000L)
        val entities = measurementDao.getListByRange(startMs, endMs)
        val measurements = entities.map { it.toDomain() }
        return csvExporter.exportToCsv(measurements)
    }

    suspend fun clearAllData() {
        measurementDao.deleteAll()
        symptomDao.deleteAll()
        _latestMeasurements.value = emptyMap()
        anomalyEngine.clear()
        alertManager.dismissActiveAlert()
    }

    suspend fun computeTriMetricAnalysis(durationDays: Int): TriMetricEnvironmentAnalysis {
        val cutoff = System.currentTimeMillis() - (durationDays * 86400_000L)
        val now = System.currentTimeMillis()
        val pressureEntities = measurementDao.getListByTypeAndRange(MeasurementType.ATMOSPHERIC_PRESSURE.name, cutoff, now)
        val hrEntities = measurementDao.getListByTypeAndRange(MeasurementType.HEART_RATE.name, cutoff, now)
        val tempEntities = measurementDao.getListByTypeAndRange(MeasurementType.BODY_TEMPERATURE.name, cutoff, now)

        return TriMetricAnalysisEngine.analyzeTriMetrics(
            pressureReadings = pressureEntities.map { it.numericValue },
            heartRateReadings = hrEntities.map { it.numericValue },
            temperatureReadings = tempEntities.map { it.numericValue },
            durationDays = durationDays
        )
    }

    suspend fun deleteOlderThan(days: Int): Int {
        val cutoff = System.currentTimeMillis() - (days * 86400_000L)
        return measurementDao.deleteOlderThan(cutoff)
    }
}
