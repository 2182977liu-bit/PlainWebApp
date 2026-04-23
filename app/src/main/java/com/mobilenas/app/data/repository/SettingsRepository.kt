package com.mobilenas.app.data.repository

import com.mobilenas.app.data.db.dao.AppSettingsDao
import com.mobilenas.app.data.db.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val dao: AppSettingsDao) {
    suspend fun get(key: String, defaultValue: String = ""): String {
        return dao.get(key)?.value ?: defaultValue
    }

    fun getFlow(key: String): Flow<AppSettingsEntity?> = dao.getFlow(key)

    suspend fun set(key: String, value: String) {
        dao.insert(AppSettingsEntity(key, value))
    }

    suspend fun getAll(): List<AppSettingsEntity> = dao.getAll()

    suspend fun setAll(settings: Map<String, String>) {
        dao.insertAll(settings.map { AppSettingsEntity(it.key, it.value) })
    }

    suspend fun delete(key: String) = dao.delete(key)
}
