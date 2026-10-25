package com.smarthealth.companion

import com.smarthealth.companion.data.ble.BleJsonParser
import com.smarthealth.companion.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BleJsonParserTest {

    private lateinit var parser: BleJsonParser

    @Before
    fun setUp() {
        parser = BleJsonParser()
    }

    @Test
    fun testParseHeartRateJson() {
        val json = """{"type": "heart_rate", "value": 76.0, "unit": "BPM", "ms": 123456}"""
        val measurements = parser.parseJsonString(json, "S3-Device")

        assertEquals(1, measurements.size)
        val m = measurements.first()
        assertEquals(MeasurementType.HEART_RATE, m.type)
        assertEquals(76.0, m.numericValue, 0.001)
        assertEquals("BPM", m.unit)
        assertEquals(123456L, m.deviceTimestampMs)
    }

    @Test
    fun testParseBodyTemperatureJson() {
        val json = """{"type": "temperature_c", "value": 36.5, "unit": "C", "ms": 123456}"""
        val measurements = parser.parseJsonString(json, "S3-Device")

        assertEquals(1, measurements.size)
        val m = measurements.first()
        assertEquals(MeasurementType.BODY_TEMPERATURE, m.type)
        assertEquals(36.5, m.numericValue, 0.001)
        assertEquals("°C", m.unit)
    }

    @Test
    fun testParseCompoundBmp180Payload() {
        val json = """{"type": "bmp180", "pressure_hpa": 1012.8, "temperature_c": 24.3, "altitude_m": 85.0, "ms": 123456}"""
        val measurements = parser.parseJsonString(json, "S3-Device")

        assertEquals(3, measurements.size)

        val pressure = measurements.find { it.type == MeasurementType.ATMOSPHERIC_PRESSURE }
        assertNotNull(pressure)
        assertEquals(1012.8, pressure!!.numericValue, 0.001)
        assertEquals("hPa", pressure.unit)

        val sensorTemp = measurements.find { it.type == MeasurementType.SENSOR_TEMPERATURE }
        assertNotNull(sensorTemp)
        assertEquals(24.3, sensorTemp!!.numericValue, 0.001)
        assertEquals("°C", sensorTemp.unit)

        val altitude = measurements.find { it.type == MeasurementType.ESTIMATED_ALTITUDE }
        assertNotNull(altitude)
        assertEquals(85.0, altitude!!.numericValue, 0.001)
        assertEquals("m", altitude.unit)
    }

    @Test
    fun testFragmentReassemblyAcrossPackets() {
        val chunk1 = """{"type": "heart_""".toByteArray(Charsets.UTF_8)
        val chunk2 = """rate", "value": 82.0, "unit": "BPM", "ms": 9999}""".toByteArray(Charsets.UTF_8)

        val res1 = parser.ingestBytes(chunk1, "S3-Device")
        assertEquals(0, res1.size) // Waiting for complete JSON

        val res2 = parser.ingestBytes(chunk2, "S3-Device")
        assertEquals(1, res2.size)
        assertEquals(MeasurementType.HEART_RATE, res2.first().type)
        assertEquals(82.0, res2.first().numericValue, 0.001)
    }

    @Test
    fun testMalformedJsonDoesNotCrash() {
        val garbage = """{invalid_json: 999!@#%^}"""
        val result = parser.parseJsonString(garbage, "S3-Device")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testDuplicateMessageRejection() {
        val json = """{"type": "heart_rate", "value": 72.0, "ms": 55555}"""
        val first = parser.parseJsonString(json, "S3-Device")
        assertEquals(1, first.size)

        val second = parser.parseJsonString(json, "S3-Device")
        assertEquals(0, second.size) // Exact duplicate rejected
    }

    @Test
    fun testArduinoExactBmp180DiscreteMessages() {
        val pressureJson = """{"type":"pressure_hpa","value":1012.80,"unit":"hPa","ms":2000}"""
        val pList = parser.parseJsonString(pressureJson, "S3 Health Monitor")
        assertEquals(1, pList.size)
        assertEquals(MeasurementType.ATMOSPHERIC_PRESSURE, pList.first().type)
        assertEquals(1012.8, pList.first().numericValue, 0.01)

        val bmpTempJson = """{"type":"bmp180_temperature_c","value":24.30,"unit":"C","ms":2001}"""
        val tList = parser.parseJsonString(bmpTempJson, "S3 Health Monitor")
        assertEquals(1, tList.size)
        assertEquals(MeasurementType.SENSOR_TEMPERATURE, tList.first().type)
        assertEquals(24.3, tList.first().numericValue, 0.01)

        val altJson = """{"type":"altitude_m","value":85.20,"unit":"m","ms":2002}"""
        val aList = parser.parseJsonString(altJson, "S3 Health Monitor")
        assertEquals(1, aList.size)
        assertEquals(MeasurementType.ESTIMATED_ALTITUDE, aList.first().type)
        assertEquals(85.2, aList.first().numericValue, 0.01)
    }

    @Test
    fun testAltitudeCalculation() {
        // Standard sea level pressure 1013.25 hPa should yield ~0m altitude
        val altAtSeaLevel = parser.calculateAltitude(1013.25, 1013.25)
        assertEquals(0.0, altAtSeaLevel, 0.5)

        // Lower pressure should yield positive altitude
        val altAtHigh = parser.calculateAltitude(900.0, 1013.25)
        assertTrue(altAtHigh > 900.0)
    }
}
