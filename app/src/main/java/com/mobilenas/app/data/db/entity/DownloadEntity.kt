package com.mobilenas.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val url: String,
    val filename: String,
    val mimeType: String = "",
    val totalSize: Long = 0,
    val downloadedSize: Long = 0,
    val progress: Int = 0,
    val status: String = "pending", // pending, downloading, completed, failed
    val filePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
