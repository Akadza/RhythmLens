package com.rimuru.android.rhythmlens.di

import com.google.firebase.auth.FirebaseAuth
import com.rimuru.android.rhythmlens.data.auth.ExternalAuthProvider
import com.rimuru.android.rhythmlens.data.auth.FirebaseAuthProviderImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindExternalAuthProvider(
        impl: FirebaseAuthProviderImpl
    ): ExternalAuthProvider

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth {
            return FirebaseAuth.getInstance()
        }
    }
}
