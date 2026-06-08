package com.rimuru.android.rhythmlens.di

import com.rimuru.android.rhythmlens.data.remote.api.PatientApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object PatientApiModule {
    @Provides
    fun patientApi(retrofit: Retrofit): PatientApi = retrofit.create(PatientApi::class.java)
}
