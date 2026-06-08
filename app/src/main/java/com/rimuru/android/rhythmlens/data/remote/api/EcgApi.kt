package com.rimuru.android.rhythmlens.data.remote.api

import com.rimuru.android.rhythmlens.data.remote.dto.DoctorConclusionDto
import com.rimuru.android.rhythmlens.data.remote.dto.EcgRecordDto
import com.rimuru.android.rhythmlens.data.remote.dto.EcgSignalDto
import com.rimuru.android.rhythmlens.data.remote.dto.SaveDoctorConclusionRequestDto
import com.rimuru.android.rhythmlens.data.remote.dto.SyntheticImageDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface EcgApi {

    @Multipart
    @POST("ecg/upload")
    suspend fun uploadEcg(
        @Part file: MultipartBody.Part,
        @Part("owner_user_id") ownerUserId: RequestBody? = null
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

    @GET("ecg/{ecg_id}/conclusion")
    suspend fun getDoctorConclusion(
        @Path("ecg_id") ecgId: String
    ): DoctorConclusionDto?

    @PUT("ecg/{ecg_id}/conclusion")
    suspend fun saveDoctorConclusion(
        @Path("ecg_id") ecgId: String,
        @Body request: SaveDoctorConclusionRequestDto
    ): DoctorConclusionDto

    @POST("ecg/{ecg_id}/synthetic-image")
    suspend fun generateSyntheticImage(
        @Path("ecg_id") ecgId: String
    ): SyntheticImageDto

    @DELETE("ecg/{ecg_id}")
    suspend fun deleteEcg(
        @Path("ecg_id") ecgId: String
    )
}
