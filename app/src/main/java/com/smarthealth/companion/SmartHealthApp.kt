package com.smarthealth.companion

import android.app.Application
import com.smarthealth.companion.alert.RealtimeAlertManager
import com.smarthealth.companion.data.ble.BleManager
import com.smarthealth.companion.data.local.AppDatabase
import com.smarthealth.companion.data.local.PreferenceManager
import com.smarthealth.companion.data.location.LocationProvider
import com.smarthealth.companion.export.CsvExporter
import com.smarthealth.companion.export.PdfReportGenerator
import com.smarthealth.companion.repository.HealthRepository

class SmartHealthApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var preferenceManager: PreferenceManager
        private set
    lateinit var bleManager: BleManager
        private set
    lateinit var locationProvider: LocationProvider
        private set
    lateinit var pdfReportGenerator: PdfReportGenerator
        private set
    lateinit var csvExporter: CsvExporter
        private set
    lateinit var alertManager: RealtimeAlertManager
        private set
    lateinit var repository: HealthRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        preferenceManager = PreferenceManager(this)
        bleManager = BleManager(this)
        locationProvider = LocationProvider(this)
        pdfReportGenerator = PdfReportGenerator(this)
        csvExporter = CsvExporter(this)
        alertManager = RealtimeAlertManager(this)

        repository = HealthRepository(
            measurementDao = database.measurementDao(),
            symptomDao = database.symptomRecordDao(),
            bleManager = bleManager,
            preferenceManager = preferenceManager,
            locationProvider = locationProvider,
            pdfReportGenerator = pdfReportGenerator,
            csvExporter = csvExporter,
            alertManager = alertManager
        )
    }
}
