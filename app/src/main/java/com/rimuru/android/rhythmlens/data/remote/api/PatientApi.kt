package com.rimuru.android.rhythmlens.data.remote.api

import com.rimuru.android.rhythmlens.data.remote.dto.AttachPatientRequestDto
import com.rimuru.android.rhythmlens.data.remote.dto.PatientDto
import com.rimuru.android.rhythmlens.data.remote.dto.PatientInviteDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PatientApi {

    @GET("patients/me/invite-code")
    suspend fun getMyInviteCode(): PatientInviteDto

    @GET("doctor/patients")
    suspend fun getDoctorPatients(): List<PatientDto>

    @POST("doctor/patients/attach")
    suspend fun attachPatient(
        @Body request: AttachPatientRequestDto
    ): PatientDto
}
