package com.rimuru.android.rhythmlens.data.repository.mapper

import com.rimuru.android.rhythmlens.data.repository.dto.DigitizedEcgDto
import com.rimuru.android.rhythmlens.data.repository.dto.EcgPointDto
import com.rimuru.android.rhythmlens.domain.model.DigitizedEcg
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgLeadOrigin
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DigitizedEcgJsonMapper @Inject constructor() {

    fun toJson(signal: DigitizedEcg?): String {
        return signal?.let { ecg ->
            JSON.encodeToString(ecg.toDto())
        } ?: EMPTY_DIGITIZED_ECG_JSON
    }

    fun fromJson(json: String): DigitizedEcg? {
        if (json.isBlank() || json == EMPTY_DIGITIZED_ECG_JSON) {
            return null
        }

        return JSON.decodeFromString<DigitizedEcgDto>(json).toDomain()
    }

    private fun DigitizedEcg.toDto(): DigitizedEcgDto {
        return DigitizedEcgDto(
            samplingRate = samplingRate,
            durationSeconds = durationSeconds,
            leads = leads.mapKeys { (lead, _) -> lead.name }.mapValues { (_, points) ->
                points.map { point -> point.toDto() }
            },
            leadOrigins = leadOrigins.mapKeys { (lead, _) -> lead.name }.mapValues { (_, origin) ->
                origin.name
            }
        )
    }

    private fun DigitizedEcgDto.toDomain(): DigitizedEcg {
        val domainLeads = leads.mapKeys { (leadName, _) ->
            EcgLead.valueOf(leadName)
        }.mapValues { (_, points) ->
            points.map { point -> point.toDomain() }
        }

        val domainOrigins = leadOrigins.mapKeys { (leadName, _) ->
            EcgLead.valueOf(leadName)
        }.mapValues { (_, originName) ->
            runCatching { EcgLeadOrigin.valueOf(originName) }.getOrDefault(EcgLeadOrigin.DIGITIZED)
        }

        return DigitizedEcg(
            leads = domainLeads,
            leadOrigins = domainLeads.keys.associateWith { lead ->
                domainOrigins[lead] ?: EcgLeadOrigin.DIGITIZED
            },
            samplingRate = samplingRate,
            durationSeconds = durationSeconds
        )
    }

    private fun EcgPoint.toDto(): EcgPointDto {
        return EcgPointDto(
            timeMs = timeMs,
            voltageMv = voltageMv
        )
    }

    private fun EcgPointDto.toDomain(): EcgPoint {
        return EcgPoint(
            timeMs = timeMs,
            voltageMv = voltageMv
        )
    }

    private companion object {
        const val EMPTY_DIGITIZED_ECG_JSON = "{}"
        val JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
