package com.mobilenas.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mobilenas.app.data.db.dao.AppSettingsDao
import com.mobilenas.app.data.db.dao.DownloadDao
import com.mobilenas.app.data.db.entity.AppSettingsEntity
import com.mobilenas.app.data.db.entity.DownloadEntity

@Database(
    entities = [DownloadEntity::class, AppSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun appSettingsDao(): AppSettingsDao
}
