package com.example.tgcloud.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    indices = [Index(value = ["fileHash"], unique = true)]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val fileHash: String,
    val fileSize: Long,
    val mimeType: String,
    val status: UploadStatus = UploadStatus.PENDING,
    val telegramFileId: String? = null,
    val telegramMessageId: Long? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val uploadedAt: Long? = null
)

enum class UploadStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED,
    SKIPPED
}