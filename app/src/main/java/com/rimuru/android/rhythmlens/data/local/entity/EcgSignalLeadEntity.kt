package com.rimuru.android.rhythmlens.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "ecg_signal_leads",
    primaryKeys = ["ecgId", "lead"],
    foreignKeys = [
        ForeignKey(
            entity = EcgRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["ecgId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ecgId"])
    ]
)
data class EcgSignalLeadEntity(
    val ecgId: String,
    val lead: String,
    val origin: String,
    val voltageSamples: ByteArray,
    val sampleCount: Int
)
