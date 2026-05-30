package com.rimuru.android.rhythmlens.domain.repository

import com.rimuru.android.rhythmlens.domain.model.Patient
import kotlinx.coroutines.flow.Flow

interface PatientRepository {

    fun observePatientsForDoctor(doctorId: String): Flow<List<Patient>>

    fun observePatientById(patientId: String): Flow<Patient?>

    suspend fun savePatient(patient: Patient)

    suspend fun getPatientByInviteCode(inviteCode: String): Patient?
}
