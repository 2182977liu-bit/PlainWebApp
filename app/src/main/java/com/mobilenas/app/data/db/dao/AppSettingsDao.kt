package com.mobilenas.app.data.db.dao

import androidx.room.*
import com.mobilenas.app.data.db.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: AppSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<AppSettingsEntity>)

    @Query("SELECT * FROM app_settings WHERE key = :key LIMIT 1")
    suspend fun get(key: String): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE key = :key LIMIT 1")
    fun getFlow(key: String): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettingsEntity>

    @Query("DELETE FROM app_settings WHERE key = :key")
    suspend fun delete(key: String)
}
