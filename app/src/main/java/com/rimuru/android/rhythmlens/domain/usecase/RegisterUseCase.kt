package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.Patient
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.AuthRepository
import com.rimuru.android.rhythmlens.domain.repository.PatientRepository
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import java.util.UUID
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(
        fullName: String,
        email: String,
        credential: String,
        role: UserRole
    ): Result<Unit> {
        return authRepository.register(
            fullName = fullName,
            email = email,
            credential = credential,
            role = role
        ).map { user ->
            val selectedPatientId = if (user.role == UserRole.PATIENT) {
                user.id
            } else {
                null
            }

            if (user.role == UserRole.PATIENT) {
                patientRepository.savePatient(
                    Patient(
                        id = user.id,
                        userId = user.id,
                        fullName = user.fullName,
                        dateOfBirth = null,
                        gender = null,
                        phone = null,
                        doctorId = null,
                        inviteCode = buildInviteCode()
                    )
                )
            }

            sessionRepository.saveSession(
                user = user,
                selectedPatientId = selectedPatientId
            )
        }
    }

    private fun buildInviteCode(): String {
        return "PAT-${UUID.randomUUID().toString().take(6).uppercase()}"
    }
}
