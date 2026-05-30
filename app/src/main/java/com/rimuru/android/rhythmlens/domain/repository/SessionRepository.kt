package com.rimuru.android.rhythmlens.domain.repository

import com.rimuru.android.rhythmlens.domain.model.User
import com.rimuru.android.rhythmlens.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    fun observeCurrentUser(): Flow<User?>

    fun observeCurrentUserRole(): Flow<UserRole?>

    fun observeSelectedPatientId(): Flow<String?>

    suspend fun saveSession(
        user: User,
        selectedPatientId: String?
    )

    suspend fun setSelectedPatientId(patientId: String?)

    suspend fun clearSession()
}
