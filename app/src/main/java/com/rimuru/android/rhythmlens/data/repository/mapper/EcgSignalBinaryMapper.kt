package com.rimuru.android.rhythmlens.data.repository.mapper

import com.rimuru.android.rhythmlens.data.local.entity.EcgSignalLeadEntity
import com.rimuru.android.rhythmlens.domain.model.DigitizedEcg
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgLeadOrigin
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

    fun toDomain(
        leadEntities: List<EcgSignalLeadEntity>,
        samplingRate: Int,
        durationSeconds: Double
    ): DigitizedEcg? {
        if (leadEntities.isEmpty()) {
            return null
        }

        val leads = leadEntities.associate { entity ->
            val lead = EcgLead.valueOf(entity.lead)
            lead to entity.toPoints(samplingRate)
        }

        val origins = leadEntities.associate { entity ->
            val lead = EcgLead.valueOf(entity.lead)
            val origin = runCatching {
                EcgLeadOrigin.valueOf(entity.origin)
            }.getOrDefault(EcgLeadOrigin.DIGITIZED)
            lead to origin
        }

        return DigitizedEcg(
            leads = leads,
            leadOrigins = origins,
            samplingRate = samplingRate,
            durationSeconds = durationSeconds
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

    private companion object {
        const val FLOAT_BYTES = 4
    }
}
