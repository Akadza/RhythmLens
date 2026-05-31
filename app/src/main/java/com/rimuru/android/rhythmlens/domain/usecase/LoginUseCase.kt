package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.data.local.dao.UserDao
import com.rimuru.android.rhythmlens.domain.model.User
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userDao: UserDao
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<Unit> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Введите email и пароль"))
        }

        val normalizedEmail = email.trim().lowercase()
        val userEntity = userDao.getByEmail(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("Пользователь с таким email не зарегистрирован"))
        val role = runCatching { UserRole.valueOf(userEntity.role) }.getOrDefault(UserRole.PATIENT)
        val user = User(
            id = userEntity.id,
            email = userEntity.email,
            fullName = userEntity.fullName,
            role = role,
            createdAt = userEntity.createdAt
        )
        val selectedPatientId = if (role == UserRole.PATIENT) {
            user.id
        } else {
            null
        }

        sessionRepository.saveSession(
            user = user,
            selectedPatientId = selectedPatientId
        )

        return Result.success(Unit)
    }
}
