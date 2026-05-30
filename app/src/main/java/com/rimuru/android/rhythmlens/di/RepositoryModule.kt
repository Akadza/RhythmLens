package com.rimuru.android.rhythmlens.di

import com.rimuru.android.rhythmlens.data.repository.DataStoreSessionRepositoryImpl
import com.rimuru.android.rhythmlens.data.repository.EcgRepositoryImpl
import com.rimuru.android.rhythmlens.data.repository.PatientRepositoryImpl
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import com.rimuru.android.rhythmlens.domain.repository.PatientRepository
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEcgRepository(
        impl: EcgRepositoryImpl
    ): EcgRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: DataStoreSessionRepositoryImpl
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindPatientRepository(
        impl: PatientRepositoryImpl
    ): PatientRepository
}
