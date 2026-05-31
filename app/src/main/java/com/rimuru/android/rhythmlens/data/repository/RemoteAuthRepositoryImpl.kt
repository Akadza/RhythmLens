package com.rimuru.android.rhythmlens.data.repository

import com.rimuru.android.rhythmlens.data.auth.ExternalAuthProvider
import com.rimuru.android.rhythmlens.data.remote.api.AuthApi
import com.rimuru.android.rhythmlens.data.remote.dto.AuthSyncRequestDto
import com.rimuru.android.rhythmlens.data.remote.dto.AuthUserDto
import com.rimuru.android.rhythmlens.domain.model.User
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.AuthRepository
import java.time.Instant
import javax.inject.Inject

class RemoteAuthRepositoryImpl @Inject constructor(
    private val externalAuthProvider: ExternalAuthProvider,
    private val authApi: AuthApi
) : AuthRepository {

    override suspend fun signIn(
        email: String,
        credential: String
    ): Result<User> {
        if (email.isBlank() || credential.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Введите email и пароль")
            )
        }

        return externalAuthProvider.signInWithEmail(
            email = email,
            credential = credential
        ).mapCatching {
            val idToken = externalAuthProvider
                .getIdToken(forceRefresh = true)
                .getOrThrow()

            authApi.login(
                AuthSyncRequestDto(
                    idToken = idToken
                )
            ).toDomain()
        }
    }

    override suspend fun register(
        fullName: String,
        email: String,
        credential: String,
        role: UserRole
    ): Result<User> {
        if (fullName.isBlank() || email.isBlank() || credential.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Заполните ФИО, email и пароль")
            )
        }

        return externalAuthProvider.registerWithEmail(
            email = email,
            credential = credential
        ).mapCatching {
            val idToken = externalAuthProvider
                .getIdToken(forceRefresh = true)
                .getOrThrow()

            authApi.register(
                AuthSyncRequestDto(
                    idToken = idToken,
                    fullName = fullName.trim(),
                    role = role.name
                )
            ).toDomain()
        }
    }

    private fun AuthUserDto.toDomain(): User {
        return User(
            id = id,
            email = email,
            fullName = fullName,
            role = runCatching {
                UserRole.valueOf(role)
            }.getOrDefault(UserRole.PATIENT),
            createdAt = runCatching {
                Instant.parse(createdAt)
            }.getOrDefault(Instant.EPOCH)
        )
    }
}