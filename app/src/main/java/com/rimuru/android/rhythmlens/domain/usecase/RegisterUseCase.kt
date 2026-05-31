package com.rimuru.android.rhythmlens.domain.usecase

import android.database.sqlite.SQLiteConstraintException
import com.rimuru.android.rhythmlens.data.local.dao.UserDao
import com.rimuru.android.rhythmlens.data.local.entity.UserEntity
import com.rimuru.android.rhythmlens.domain.model.Patient
import com.rimuru.android.rhythmlens.domain.model.User
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.PatientRepository
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val patientRepository: PatientRepository,
    private val userDao: UserDao
) {
    suspend operator fun invoke(
        fullName: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<Unit> {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Заполните ФИО, email и пароль"))
        }

        val normalizedEmail = email.trim().lowercase()
        val now = Instant.now()
        val user = User(
            id = UUID.randomUUID().toString(),
            email = normalizedEmail,
            fullName = fullName.trim(),
            role = role,
            createdAt = now
        )

        runCatching {
            userDao.insert(
                UserEntity(
                    id = user.id,
                    email = user.email,
                    fullName = user.fullName,
                    role = user.role.name,
                    createdAt = user.createdAt
                )
            )
        }.onFailure { throwable ->
            if (throwable is SQLiteConstraintException) {
                return Result.failure(IllegalArgumentException("Пользователь с таким email уже зарегистрирован"))
            }
            return Result.failure(throwable)
        }

        val selectedPatientId = if (role == UserRole.PATIENT) {
            user.id
        } else {
            null
        }

        if (role == UserRole.PATIENT) {
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

        return Result.success(Unit)
    }

    private fun buildInviteCode(): String {
        return "PAT-${UUID.randomUUID().toString().take(6).uppercase()}"
    }
}
