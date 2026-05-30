package com.rimuru.android.rhythmlens.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rimuru.android.rhythmlens.data.local.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: PatientEntity)

    @Query("SELECT * FROM patients WHERE doctorId = :doctorId ORDER BY fullName ASC")
    fun observePatientsForDoctor(doctorId: String): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    fun observePatientById(patientId: String): Flow<PatientEntity?>

    @Query("SELECT * FROM patients WHERE inviteCode = :inviteCode LIMIT 1")
    suspend fun getByInviteCode(inviteCode: String): PatientEntity?
}
