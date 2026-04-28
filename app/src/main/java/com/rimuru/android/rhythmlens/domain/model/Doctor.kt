package com.rimuru.android.rhythmlens.domain.model

data class Doctor (
    val id: String,
    val userId: String,
    val fullName: String,
    val specialization: String?,
    val licenseNumber: String?
)
