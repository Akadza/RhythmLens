package com.rimuru.android.rhythmlens.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rimuru.android.rhythmlens.data.local.entity.EcgSignalLeadEntity
import com.rimuru.android.rhythmlens.data.local.entity.EcgSignalSegmentEntity

@Dao
interface EcgSignalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(leads: List<EcgSignalLeadEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<EcgSignalSegmentEntity>)

    @Query("SELECT * FROM ecg_signal_leads WHERE ecgId = :ecgId")
    suspend fun getLeadsForEcg(ecgId: String): List<EcgSignalLeadEntity>

    @Query("SELECT * FROM ecg_signal_segments WHERE ecgId = :ecgId ORDER BY lead, segmentIndex")
    suspend fun getSegmentsForEcg(ecgId: String): List<EcgSignalSegmentEntity>

    @Query("DELETE FROM ecg_signal_leads WHERE ecgId = :ecgId")
    suspend fun deleteLeadsForEcg(ecgId: String)

    @Query("DELETE FROM ecg_signal_segments WHERE ecgId = :ecgId")
    suspend fun deleteSegmentsForEcg(ecgId: String)
}
