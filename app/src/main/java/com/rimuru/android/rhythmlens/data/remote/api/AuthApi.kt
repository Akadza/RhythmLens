package com.rimuru.android.rhythmlens.data.remote.api

import com.rimuru.android.rhythmlens.data.remote.dto.AuthSyncRequestDto
import com.rimuru.android.rhythmlens.data.remote.dto.AuthUserDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: AuthSyncRequestDto
    ): AuthUserDto

    @POST("auth/register")
    suspend fun register(
        @Body request: AuthSyncRequestDto
    ): AuthUserDto
}
