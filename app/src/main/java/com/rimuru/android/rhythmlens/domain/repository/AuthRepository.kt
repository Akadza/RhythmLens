package com.rimuru.android.rhythmlens.domain.repository

import com.rimuru.android.rhythmlens.domain.model.User
import com.rimuru.android.rhythmlens.domain.model.UserRole

interface AuthRepository {

    suspend fun signIn(
        email: String,
        credential: String
    ): Result<User>

    suspend fun register(
        fullName: String,
        email: String,
        credential: String,
        role: UserRole
    ): Result<User>
}
