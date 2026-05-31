package com.rimuru.android.rhythmlens.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rimuru.android.rhythmlens.domain.model.User
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "session")

@Singleton
class DataStoreSessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionRepository {

    override fun observeCurrentUser(): Flow<User?> {
        return context.sessionDataStore.data.map { preferences ->
            val id = preferences[USER_ID] ?: return@map null
            val email = preferences[USER_EMAIL].orEmpty()
            val fullName = preferences[USER_FULL_NAME].orEmpty()
            val role = preferences[USER_ROLE]
                ?.let { roleName -> runCatching { UserRole.valueOf(roleName) }.getOrNull() }
                ?: UserRole.PATIENT
            val createdAt = preferences[USER_CREATED_AT]
                ?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
                ?: Instant.EPOCH

            User(
                id = id,
                email = email,
                fullName = fullName,
                role = role,
                createdAt = createdAt
            )
        }
    }

    override fun observeCurrentUserRole(): Flow<UserRole?> {
        return observeCurrentUser().map { user -> user?.role }
    }

    override fun observeSelectedPatientId(): Flow<String?> {
        return context.sessionDataStore.data.map { preferences ->
            preferences[SELECTED_PATIENT_ID]
        }
    }

    override suspend fun saveSession(
        user: User,
        selectedPatientId: String?
    ) {
        context.sessionDataStore.edit { preferences ->
            preferences[USER_ID] = user.id
            preferences[USER_EMAIL] = user.email
            preferences[USER_FULL_NAME] = user.fullName
            preferences[USER_ROLE] = user.role.name
            preferences[USER_CREATED_AT] = user.createdAt.toString()

            if (selectedPatientId == null) {
                preferences.remove(SELECTED_PATIENT_ID)
            } else {
                preferences[SELECTED_PATIENT_ID] = selectedPatientId
            }
        }
    }

    override suspend fun setSelectedPatientId(patientId: String?) {
        context.sessionDataStore.edit { preferences ->
            if (patientId == null) {
                preferences.remove(SELECTED_PATIENT_ID)
            } else {
                preferences[SELECTED_PATIENT_ID] = patientId
            }
        }
    }

    override suspend fun clearSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_FULL_NAME = stringPreferencesKey("user_full_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_CREATED_AT = stringPreferencesKey("user_created_at")
        val SELECTED_PATIENT_ID = stringPreferencesKey("selected_patient_id")
    }
}
