package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.PatientRepository
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AttachPatientByInviteCodeUseCase @Inject constructor(
    private val patientRepository: PatientRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(inviteCode: String): Result<String> {
        return runCatching {
            val doctor = sessionRepository.observeCurrentUser().first()
                ?: throw IllegalStateException("Пользователь не найден")

            if (doctor.role != UserRole.DOCTOR) {
                throw IllegalStateException("Действие доступно только врачу")
            }

            val patient = patientRepository.attachPatientByInviteCode(
                inviteCode = inviteCode,
                doctorId = doctor.id
            )

            sessionRepository.setSelectedPatientId(patient.id)

            patient.id
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.toAttachPatientMessage(), throwable)
        }
    }

    private fun Throwable.toAttachPatientMessage(): String {
        val rawMessage = message.orEmpty()
        return when {
            rawMessage.contains("404") || rawMessage.contains("not found", ignoreCase = true) -> {
                "Пациент с таким кодом не найден"
            }

            rawMessage.contains("403") || rawMessage.contains("forbidden", ignoreCase = true) -> {
                "Недостаточно прав для добавления пациента"
            }

            rawMessage.isBlank() -> {
                "Не удалось добавить пациента по коду"
            }

            else -> rawMessage
        }
    }
}
