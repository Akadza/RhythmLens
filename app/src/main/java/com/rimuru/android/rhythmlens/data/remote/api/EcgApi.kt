package com.rimuru.android.rhythmlens.data.remote.api

import com.rimuru.android.rhythmlens.data.remote.dto.EcgRecordDto
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface EcgApi {

    @Multipart
    @POST("ecg/upload")
    suspend fun uploadEcg(
        @Part file: MultipartBody.Part
    ): EcgRecordDto

    @GET("ecg")
    suspend fun getEcgRecords(): List<EcgRecordDto>

    @GET("ecg/{ecg_id}")
    suspend fun getEcgById(
        @Path("ecg_id") ecgId: String
    ): EcgRecordDto
}
