package com.rimuru.android.rhythmlens.data.auth

interface ExternalAuthProvider {

    suspend fun signInWithEmail(
        email: String,
        credential: String
    ): Result<ExternalAuthUser>

    suspend fun registerWithEmail(
        email: String,
        credential: String
    ): Result<ExternalAuthUser>

    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String>

    suspend fun signOut()
}
