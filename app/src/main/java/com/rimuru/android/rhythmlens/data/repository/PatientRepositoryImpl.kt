package com.rimuru.android.rhythmlens.data.repository

import com.rimuru.android.rhythmlens.data.local.dao.PatientDao
import com.rimuru.android.rhythmlens.data.local.entity.PatientEntity
import com.rimuru.android.rhythmlens.domain.model.Gender
import com.rimuru.android.rhythmlens.domain.model.Patient
import com.rimuru.android.rhythmlens.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PatientRepositoryImpl @Inject constructor(
    private val patientDao: PatientDao
) : PatientRepository {

    override fun observePatientsForDoctor(doctorId: String): Flow<List<Patient>> {
        return patientDao.observePatientsForDoctor(doctorId).map { patients ->
            patients.map { patient -> patient.toDomain() }
        }
    }

    override fun observePatientById(patientId: String): Flow<Patient?> {
        return patientDao.observePatientById(patientId).map { patient ->
            patient?.toDomain()
        }
    }

    override suspend fun savePatient(patient: Patient) {
        patientDao.insert(patient.toEntity())
    }

    override suspend fun getPatientByInviteCode(inviteCode: String): Patient? {
        return patientDao.getByInviteCode(inviteCode)?.toDomain()
    }

    private fun PatientEntity.toDomain(): Patient {
        return Patient(
            id = id,
            userId = userId,
            fullName = fullName,
            dateOfBirth = dateOfBirth,
            gender = gender?.let { genderName ->
                runCatching { Gender.valueOf(genderName) }.getOrNull()
            },
            phone = phone,
            doctorId = doctorId,
            inviteCode = inviteCode
        )
    }

    private fun Patient.toEntity(): PatientEntity {
        return PatientEntity(
            id = id,
            userId = userId,
            fullName = fullName,
            dateOfBirth = dateOfBirth,
            gender = gender?.name,
            phone = phone,
            doctorId = doctorId,
            inviteCode = inviteCode
        )
    }
}
