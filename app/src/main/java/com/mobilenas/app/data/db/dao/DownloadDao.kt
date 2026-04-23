package com.mobilenas.app.data.db.dao

import androidx.room.*
import com.mobilenas.app.data.db.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    suspend fun getAllDownloadsOnce(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE taskId = :taskId LIMIT 1")
    suspend fun getByTaskId(taskId: String): DownloadEntity?

    @Query("DELETE FROM downloads WHERE status IN ('completed', 'failed')")
    suspend fun deleteFinishedDownloads()

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'downloading'")
    suspend fun getActiveDownloadCount(): Int
}
