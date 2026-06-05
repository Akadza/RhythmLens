package com.rimuru.android.rhythmlens.di

import com.rimuru.android.rhythmlens.data.auth.ExternalAuthProvider
import com.rimuru.android.rhythmlens.data.remote.api.AuthApi
import com.rimuru.android.rhythmlens.data.remote.api.EcgApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://rhythmlens.tplinkdns.com:8443/"

    @Provides
    @Singleton
    fun provideOkHttpClient(
        externalAuthProvider: ExternalAuthProvider
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val token = runBlocking {
                    externalAuthProvider.getIdToken(forceRefresh = false).getOrNull()
                }

                val request = if (token.isNullOrBlank()) {
                    originalRequest
                } else {
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                }

                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.MINUTES)
            .callTimeout(30, TimeUnit.MINUTES)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideEcgApi(retrofit: Retrofit): EcgApi {
        return retrofit.create(EcgApi::class.java)
    }
}
