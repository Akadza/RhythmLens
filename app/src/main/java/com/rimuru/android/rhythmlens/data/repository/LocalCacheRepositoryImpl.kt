package com.rimuru.android.rhythmlens.data.repository

import com.rimuru.android.rhythmlens.data.local.RhythmLensDatabase
import com.rimuru.android.rhythmlens.domain.repository.LocalCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCacheRepositoryImpl @Inject constructor(
    private val database: RhythmLensDatabase
) : LocalCacheRepository {

    override suspend fun clearAllLocalData() {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
    }
}
