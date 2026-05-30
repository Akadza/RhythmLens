package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.repository.PatientRepository
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AttachPatientByInviteCodeUseCase @Inject constructor(
    private val patientRepository: PatientRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(inviteCode: String): Result<String> {
        val doctor = sessionRepository.observeCurrentUser().first()
            ?: return Result.failure(IllegalStateException("Текущий пользователь не найден"))
        val normalizedCode = inviteCode.trim().uppercase()
        val patient = patientRepository.getPatientByInviteCode(normalizedCode)
            ?: return Result.failure(IllegalArgumentException("Пациент с таким кодом не найден"))

        val linkedPatient = patient.copy(doctorId = doctor.id)
        patientRepository.savePatient(linkedPatient)
        sessionRepository.setSelectedPatientId(linkedPatient.id)

        return Result.success(linkedPatient.id)
    }
}
