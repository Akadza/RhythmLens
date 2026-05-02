package com.rimuru.android.rhythmlens.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rimuru.android.rhythmlens.data.local.entity.EcgRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EcgDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ecg: EcgRecordEntity)

    @Query("SELECT * FROM ecg_records WHERE id = :id")
    fun getById(id: String): Flow<EcgRecordEntity?>

    @Query("SELECT * FROM ecg_records WHERE patientId = :patientId ORDER BY recordedAt DESC")
    fun getAllForPatient(patientId: String): Flow<List<EcgRecordEntity>>

    @Query("DELETE FROM ecg_records WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM ecg_records")
    fun getAll(): Flow<List<EcgRecordEntity>>
}