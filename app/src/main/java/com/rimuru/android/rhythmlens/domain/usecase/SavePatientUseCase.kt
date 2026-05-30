package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.Patient
import com.rimuru.android.rhythmlens.domain.repository.PatientRepository
import javax.inject.Inject

class SavePatientUseCase @Inject constructor(
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(patient: Patient) {
        patientRepository.savePatient(patient)
    }
}
