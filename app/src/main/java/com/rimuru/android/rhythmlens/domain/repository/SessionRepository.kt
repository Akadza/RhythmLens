package com.rimuru.android.rhythmlens.domain.repository

import com.rimuru.android.rhythmlens.domain.model.User
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeCurrentUser(): Flow<User>
    fun observeSelectedPatientId(): Flow<String>
    suspend fun setCurrentUser(user: User)
    suspend fun setSelectedPatientId(patientId: String)
}
