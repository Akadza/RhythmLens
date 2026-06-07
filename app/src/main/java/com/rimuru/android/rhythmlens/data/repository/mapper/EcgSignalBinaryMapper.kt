package com.rimuru.android.rhythmlens.data.repository.mapper

import com.rimuru.android.rhythmlens.data.local.entity.EcgSignalLeadEntity
import com.rimuru.android.rhythmlens.data.local.entity.EcgSignalSegmentEntity
import com.rimuru.android.rhythmlens.domain.model.DigitizedEcg
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgLeadOrigin
import com.rimuru.android.rhythmlens.domain.model.EcgLeadSegment
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

class EcgSignalBinaryMapper @Inject constructor() {

    fun toEntities(ecgId: String, signal: DigitizedEcg): List<EcgSignalLeadEntity> {
        return signal.leads.map { (lead, points) ->
            EcgSignalLeadEntity(
                ecgId = ecgId,
                lead = lead.name,
                origin = (signal.leadOrigins[lead] ?: EcgLeadOrigin.DIGITIZED).name,
                voltageSamples = points.toVoltageByteArray(),
                sampleCount = points.size
            )
        }
    }

    fun toSegmentEntities(ecgId: String, signal: DigitizedEcg): List<EcgSignalSegmentEntity> {
        return signal.leadSegments.flatMap { (lead, segments) ->
            segments.mapIndexed { index, segment ->
                EcgSignalSegmentEntity(
                    ecgId = ecgId,
                    lead = lead.name,
                    segmentIndex = index,
                    origin = segment.origin.name,
                    startSampleIndex = segment.startSampleIndex,
                    voltageSamples = segment.points.toVoltageByteArray(),
                    sampleCount = segment.points.size
                )
            }
        }
    }

    fun toDomain(
        leadEntities: List<EcgSignalLeadEntity>,
        segmentEntities: List<EcgSignalSegmentEntity> = emptyList(),
        samplingRate: Int,
        durationSeconds: Double
    ): DigitizedEcg? {
        if (leadEntities.isEmpty() && segmentEntities.isEmpty()) {
            return null
        }

        val segments = segmentEntities
            .groupBy { entity -> EcgLead.valueOf(entity.lead) }
            .mapValues { (_, entities) ->
                entities
                    .sortedBy { entity -> entity.segmentIndex }
                    .map { entity -> entity.toSegment(samplingRate) }
            }

        val leads = if (leadEntities.isNotEmpty()) {
            leadEntities.associate { entity ->
                val lead = EcgLead.valueOf(entity.lead)
                lead to entity.toPoints(samplingRate)
            }
        } else {
            segments.mapValues { (_, leadSegments) ->
                leadSegments
                    .sortedBy { segment -> segment.startSampleIndex }
                    .flatMap { segment -> segment.points }
            }
        }

        val origins = if (leadEntities.isNotEmpty()) {
            leadEntities.associate { entity ->
                val lead = EcgLead.valueOf(entity.lead)
                val origin = runCatching {
                    EcgLeadOrigin.valueOf(entity.origin)
                }.getOrDefault(EcgLeadOrigin.DIGITIZED)
                lead to origin
            }
        } else {
            segments.mapValues { (_, leadSegments) ->
                leadSegments.toLeadOrigin()
            }
        }

        return DigitizedEcg(
            leads = leads,
            leadOrigins = origins,
            samplingRate = samplingRate,
            durationSeconds = durationSeconds,
            leadSegments = segments.ifEmpty {
                leads.mapValues { (lead, points) ->
                    listOf(
                        EcgLeadSegment(
                            origin = origins[lead] ?: EcgLeadOrigin.DIGITIZED,
                            startSampleIndex = 0,
                            points = points
                        )
                    )
                }
            }
        )
    }

    private fun List<EcgPoint>.toVoltageByteArray(): ByteArray {
        val buffer = ByteBuffer
            .allocate(size * FLOAT_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)

        forEach { point ->
            buffer.putFloat(point.voltageMv.toFloat())
        }

        return buffer.array()
    }

    private fun EcgSignalLeadEntity.toPoints(samplingRate: Int): List<EcgPoint> {
        val buffer = ByteBuffer
            .wrap(voltageSamples)
            .order(ByteOrder.LITTLE_ENDIAN)
        val count = minOf(sampleCount, voltageSamples.size / FLOAT_BYTES)

        return List(count) { index ->
            EcgPoint(
                timeMs = index * 1000L / samplingRate,
                voltageMv = buffer.getFloat().toDouble()
            )
        }
    }

    private fun EcgSignalSegmentEntity.toSegment(samplingRate: Int): EcgLeadSegment {
        val buffer = ByteBuffer
            .wrap(voltageSamples)
            .order(ByteOrder.LITTLE_ENDIAN)
        val count = minOf(sampleCount, voltageSamples.size / FLOAT_BYTES)
        val origin = runCatching {
            EcgLeadOrigin.valueOf(origin)
        }.getOrDefault(EcgLeadOrigin.RECONSTRUCTED)

        return EcgLeadSegment(
            origin = origin,
            startSampleIndex = startSampleIndex,
            points = List(count) { index ->
                val sampleIndex = startSampleIndex + index
                EcgPoint(
                    timeMs = sampleIndex * 1000L / samplingRate,
                    voltageMv = buffer.getFloat().toDouble()
                )
            }
        )
    }

    private fun List<EcgLeadSegment>.toLeadOrigin(): EcgLeadOrigin {
        val origins = map { segment -> segment.origin }.toSet()
        return when {
            origins == setOf(EcgLeadOrigin.DIGITIZED) -> EcgLeadOrigin.DIGITIZED
            origins == setOf(EcgLeadOrigin.RECONSTRUCTED) -> EcgLeadOrigin.RECONSTRUCTED
            origins.isEmpty() -> EcgLeadOrigin.RECONSTRUCTED
            else -> EcgLeadOrigin.MIXED
        }
    }

    private companion object {
        const val FLOAT_BYTES = 4
    }
}
