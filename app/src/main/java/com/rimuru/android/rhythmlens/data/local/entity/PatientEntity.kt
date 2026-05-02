package com.rimuru.android.rhythmlens.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val fullName: String,
    val dateOfBirth: LocalDate?,
    val gender: String?,
    val phone: String?,
    val doctorId: String?
)