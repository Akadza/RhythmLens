package com.rimuru.android.rhythmlens.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientDto(
    val id: String,
    val email: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("invite_code")
    val inviteCode: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class PatientInviteDto(
    @SerialName("invite_code")
    val inviteCode: String
)

@Serializable
data class AttachPatientRequestDto(
    @SerialName("invite_code")
    val inviteCode: String
)
