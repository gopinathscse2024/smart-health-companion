package com.smarthealth.companion.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomRecordDao {

    @Query("SELECT * FROM symptom_records ORDER BY timestampMs DESC")
    fun getAllSymptoms(): Flow<List<SymptomRecordEntity>>

    @Query("SELECT * FROM symptom_records WHERE timestampMs BETWEEN :startMs AND :endMs ORDER BY timestampMs ASC")
    fun getSymptomsByRange(startMs: Long, endMs: Long): Flow<List<SymptomRecordEntity>>

    @Query("SELECT * FROM symptom_records WHERE timestampMs BETWEEN :startMs AND :endMs ORDER BY timestampMs ASC")
    suspend fun getListByRange(startMs: Long, endMs: Long): List<SymptomRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SymptomRecordEntity)

    @Query("DELETE FROM symptom_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM symptom_records")
    suspend fun deleteAll()
}
