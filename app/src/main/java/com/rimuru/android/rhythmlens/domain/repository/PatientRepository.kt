package com.rimuru.android.rhythmlens.domain.repository

import com.rimuru.android.rhythmlens.domain.model.Patient
import kotlinx.coroutines.flow.Flow

interface PatientRepository {

    fun observePatientsForDoctor(doctorId: String): Flow<List<Patient>>

    fun observePatientById(patientId: String): Flow<Patient?>

    suspend fun savePatient(patient: Patient)

    suspend fun refreshPatientsForDoctor(doctorId: String): List<Patient>

    suspend fun attachPatientByInviteCode(inviteCode: String, doctorId: String): Patient

    suspend fun getMyInviteCode(): String

    suspend fun getPatientByInviteCode(inviteCode: String): Patient?
}
