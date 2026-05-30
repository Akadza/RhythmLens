package com.rimuru.android.rhythmlens.domain.model

import java.time.Instant

data class DoctorConclusion(
    val ecgId: String,
    val doctorId: String,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
