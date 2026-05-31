package com.rimuru.android.rhythmlens.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthProviderImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ExternalAuthProvider {

    override suspend fun signInWithEmail(
        email: String,
        credential: String
    ): Result<ExternalAuthUser> {
        return runCatching {
            val result = firebaseAuth
                .signInWithEmailAndPassword(email.trim(), credential)
                .await()
            val firebaseUser = result.user
                ?: error("Firebase user is empty")

            ExternalAuthUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email
            )
        }
    }

    override suspend fun registerWithEmail(
        email: String,
        credential: String
    ): Result<ExternalAuthUser> {
        return runCatching {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email.trim(), credential)
                .await()
            val firebaseUser = result.user
                ?: error("Firebase user is empty")

            ExternalAuthUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email
            )
        }
    }

    override suspend fun getIdToken(forceRefresh: Boolean): Result<String> {
        return runCatching {
            val firebaseUser = firebaseAuth.currentUser
                ?: error("Firebase user is not signed in")

            firebaseUser
                .getIdToken(forceRefresh)
                .await()
                .token
                ?: error("Firebase ID token is empty")
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }
}