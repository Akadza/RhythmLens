package com.rimuru.android.rhythmlens.di

import com.rimuru.android.rhythmlens.data.repository.EcgRepositoryImpl
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
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
}