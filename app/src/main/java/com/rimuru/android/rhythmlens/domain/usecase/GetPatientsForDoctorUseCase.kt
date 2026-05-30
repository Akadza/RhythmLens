package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.Patient
import com.rimuru.android.rhythmlens.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPatientsForDoctorUseCase @Inject constructor(
    private val patientRepository: PatientRepository
) {
    operator fun invoke(doctorId: String): Flow<List<Patient>> {
        return patientRepository.observePatientsForDoctor(doctorId)
    }
}
