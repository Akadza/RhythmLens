package com.rimuru.android.rhythmlens.domain.model

import android.provider.ContactsContract
import java.time.Instant

data class User (
    val id: String,
    val email: String,
    val role: UserRole,
    val createdAt: Instant = Instant.now()
)
