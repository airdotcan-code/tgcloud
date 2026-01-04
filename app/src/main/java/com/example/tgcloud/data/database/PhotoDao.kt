package com.example.tgcloud.data.database

import androidx.room.*
import com.example.tgcloud.data.database.entities.PhotoEntity
import com.example.tgcloud.data.database.entities.UploadStatus
import com.example.tgcloud.data.database.entities.WatchFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos ORDER BY createdAt DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE status = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPhotosByStatus(status: UploadStatus, limit: Int = 50): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE status = 'PENDING' AND retryCount < 3 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingPhotos(limit: Int): List<PhotoEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM photos WHERE fileHash = :hash)")
    suspend fun existsByHash(hash: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("""
        UPDATE photos 
        SET status = :status, uploadedAt = :uploadedAt, 
            telegramFileId = :fileId, telegramMessageId = :messageId 
        WHERE id = :id
    """)
    suspend fun markAsCompleted(
        id: Long,
        status: UploadStatus = UploadStatus.COMPLETED,
        uploadedAt: Long = System.currentTimeMillis(),
        fileId: String?,
        messageId: Long?
    )

    @Query("UPDATE photos SET status = :status, errorMessage = :error, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markAsFailed(
        id: Long,
        status: UploadStatus = UploadStatus.FAILED,
        error: String?
    )

    @Query("UPDATE photos SET status = 'PENDING', retryCount = 0 WHERE status = 'FAILED'")
    suspend fun resetFailed()

    @Query("SELECT COUNT(*) FROM photos WHERE status = :status")
    suspend fun getCountByStatus(status: UploadStatus): Int

    @Query("SELECT COUNT(*) FROM photos")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT SUM(fileSize) FROM photos WHERE status = 'COMPLETED'")
    fun getTotalUploadedSize(): Flow<Long?>

    // Watch Folders
    @Query("SELECT * FROM watch_folders ORDER BY displayName")
    fun getAllFolders(): Flow<List<WatchFolderEntity>>

    @Query("SELECT * FROM watch_folders WHERE isEnabled = 1")
    suspend fun getEnabledFolders(): List<WatchFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: WatchFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: WatchFolderEntity)

    @Delete
    suspend fun deleteFolder(folder: WatchFolderEntity)

    @Query("UPDATE watch_folders SET lastScanAt = :timestamp WHERE id = :id")
    suspend fun updateFolderScanTime(id: Long, timestamp: Long = System.currentTimeMillis())
}