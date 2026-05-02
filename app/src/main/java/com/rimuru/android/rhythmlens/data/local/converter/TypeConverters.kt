package com.rimuru.android.rhythmlens.data.local.converter

import androidx.room.TypeConverter
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    // Для Map<EcgLead, List<EcgPoint>>
    @TypeConverter
    fun fromDigitizedSignal(value: Map<EcgLead, List<EcgPoint>>?): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toDigitizedSignal(value: String?): Map<EcgLead, List<EcgPoint>>? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String?): List<String> = value?.let { json.decodeFromString(it) } ?: emptyList()
}