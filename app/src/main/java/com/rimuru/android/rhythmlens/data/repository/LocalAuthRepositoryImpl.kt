package com.rimuru.android.rhythmlens.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.rimuru.android.rhythmlens.data.local.dao.UserDao
import com.rimuru.android.rhythmlens.data.local.entity.UserEntity
import com.rimuru.android.rhythmlens.domain.model.User
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.AuthRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class LocalAuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun signIn(
        email: String,
        credential: String
    ): Result<User> {
        if (email.isBlank() || credential.isBlank()) {
            return Result.failure(IllegalArgumentException("Введите email и пароль"))
        }

        val normalizedEmail = email.trim().lowercase()
        val userEntity = userDao.getByEmail(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("Пользователь с таким email не зарегистрирован"))

        return Result.success(userEntity.toDomain())
    }

    override suspend fun register(
        fullName: String,
        email: String,
        credential: String,
        role: UserRole
    ): Result<User> {
        if (fullName.isBlank() || email.isBlank() || credential.isBlank()) {
            return Result.failure(IllegalArgumentException("Заполните ФИО, email и пароль"))
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            email = email.trim().lowercase(),
            fullName = fullName.trim(),
            role = role,
            createdAt = Instant.now()
        )

        runCatching {
            userDao.insert(user.toEntity())
        }.onFailure { throwable ->
            if (throwable is SQLiteConstraintException) {
                return Result.failure(IllegalArgumentException("Пользователь с таким email уже зарегистрирован"))
            }
            return Result.failure(throwable)
        }

        return Result.success(user)
    }

    override suspend fun signOut() = Unit

    private fun UserEntity.toDomain(): User {
        return User(
            id = id,
            email = email,
            fullName = fullName,
            role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.PATIENT),
            createdAt = createdAt
        )
    }

    private fun User.toEntity(): UserEntity {
        return UserEntity(
            id = id,
            email = email,
            fullName = fullName,
            role = role.name,
            createdAt = createdAt
        )
    }
}
