package com.rimuru.android.rhythmlens.data.remote.api

import com.rimuru.android.rhythmlens.data.remote.dto.EcgRecordDto
import com.rimuru.android.rhythmlens.data.remote.dto.EcgSignalDto
import okhttp3.MultipartBody
import retrofit2.http.DELETE
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

    @GET("ecg/{ecg_id}/signal")
    suspend fun getEcgSignal(
        @Path("ecg_id") ecgId: String
    ): EcgSignalDto

    @DELETE("ecg/{ecg_id}")
    suspend fun deleteEcg(
        @Path("ecg_id") ecgId: String
    )
}
