package com.rimuru.android.rhythmlens.domain.repository

interface LocalCacheRepository {

    suspend fun clearAllLocalData()
}
