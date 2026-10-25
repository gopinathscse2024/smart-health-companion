package com.smarthealth.companion.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Query("SELECT * FROM measurements ORDER BY phoneReceiptTimestampMs DESC LIMIT :limit")
    fun getRecentMeasurements(limit: Int): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE type = :type ORDER BY phoneReceiptTimestampMs DESC LIMIT 1")
    fun getLatestByType(type: String): Flow<MeasurementEntity?>

    @Query("SELECT * FROM measurements WHERE type = :type AND phoneReceiptTimestampMs BETWEEN :startMs AND :endMs ORDER BY phoneReceiptTimestampMs ASC")
    fun getMeasurementsByTypeAndRange(type: String, startMs: Long, endMs: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE phoneReceiptTimestampMs BETWEEN :startMs AND :endMs ORDER BY phoneReceiptTimestampMs DESC")
    fun getAllMeasurementsByRange(startMs: Long, endMs: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE type = :type AND phoneReceiptTimestampMs BETWEEN :startMs AND :endMs ORDER BY phoneReceiptTimestampMs ASC")
    suspend fun getListByTypeAndRange(type: String, startMs: Long, endMs: Long): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE phoneReceiptTimestampMs BETWEEN :startMs AND :endMs ORDER BY phoneReceiptTimestampMs ASC")
    suspend fun getListByRange(startMs: Long, endMs: Long): List<MeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MeasurementEntity>)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM measurements WHERE phoneReceiptTimestampMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("DELETE FROM measurements")
    suspend fun deleteAll()

    @Query("DELETE FROM measurements WHERE source = :source")
    suspend fun deleteBySource(source: String): Int

    @Query("SELECT COUNT(*) FROM measurements")
    fun getCount(): Flow<Int>
}
