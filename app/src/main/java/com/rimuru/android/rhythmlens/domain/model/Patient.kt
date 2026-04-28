package com.rimuru.android.rhythmlens.domain.model

import java.time.LocalDate

data class Patient (
    val id: String,
    val userId: String,
    val fullName: String,
    val dateOfBirth: LocalDate,
    val gender: Gender,
    val phone: String?,
    val doctorId: String? = null
)

enum class Gender {
    MALE, FEMALE
}