package com.rimuru.android.rhythmlens.data.repository

import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class EcgRepositoryImpl @Inject constructor(
    // private val localDao: EcgDao,           // позже
    // private val api: EcgApi                  // позже
) : EcgRepository {

    override suspend fun digitizeEcg(imageUri: String): EcgRecord {
        // Пока заглушка. Позже здесь будет multipart-запрос на сервер
        TODO("Implement server call for digitization")
    }

    override suspend fun saveEcg(record: EcgRecord) {
        // localDao.insert(...) + отправка на сервер
        TODO("Save to local DB and sync with server")
    }

    override fun getEcgById(id: String): Flow<EcgRecord?> {
        // return localDao.getById(id)
        return flow { emit(null) } // заглушка
    }

    override fun getAllEcgForPatient(patientId: String): Flow<List<EcgRecord>> {
        return flow { emit(emptyList()) } // заглушка
    }

    override suspend fun deleteEcg(id: String) {
        // localDao.delete(id)
    }

    override suspend fun generateSyntheticImage(ecgId: String): String {
        // TODO: запрос на сервер для генерации PNG
        return "https://fake-url.com/synthetic.png"
    }
}