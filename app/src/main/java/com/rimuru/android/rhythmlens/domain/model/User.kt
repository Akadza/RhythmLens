package com.rimuru.android.rhythmlens.domain.model

import java.time.Instant

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val createdAt: Instant = Instant.now()
)
