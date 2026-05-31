package com.rimuru.android.rhythmlens.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthSyncRequestDto(
    @SerialName("id_token")
    val idToken: String,
    @SerialName("full_name")
    val fullName: String? = null,
    val role: String? = null
)

@Serializable
data class AuthUserDto(
    val id: String,
    @SerialName("firebase_uid")
    val firebaseUid: String,
    val email: String,
    @SerialName("full_name")
    val fullName: String,
    val role: String,
    @SerialName("created_at")
    val createdAt: String
)
