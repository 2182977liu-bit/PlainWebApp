package com.mobilenas.app.data.repository

import com.mobilenas.app.data.db.dao.DownloadDao
import com.mobilenas.app.data.db.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val dao: DownloadDao) {
    fun getAllDownloads(): Flow<List<DownloadEntity>> = dao.getAllDownloads()

    suspend fun getAllDownloadsOnce(): List<DownloadEntity> = dao.getAllDownloadsOnce()

    suspend fun insert(download: DownloadEntity): Long = dao.insert(download)

    suspend fun update(download: DownloadEntity) = dao.update(download)

    suspend fun delete(download: DownloadEntity) = dao.delete(download)

    suspend fun getByTaskId(taskId: String): DownloadEntity? = dao.getByTaskId(taskId)

    suspend fun deleteFinished() = dao.deleteFinishedDownloads()

    suspend fun getActiveCount(): Int = dao.getActiveDownloadCount()
}
